package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.monostudio.payment.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.monostudio.api.models.CheckoutStartRequest;
import org.monostudio.api.models.CheckoutOtpInitiateResponse;
import org.monostudio.api.models.CheckoutOtpResendRequest;
import org.monostudio.api.models.CheckoutOtpVerifyRequest;
import org.monostudio.api.models.PaymentRedirectionDetailsPojo;
import org.monostudio.api.models.OrderPojo;
import org.monostudio.api.services.CheckoutService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.payment.PaymentServiceException;
import org.monostudio.payment.impl.payos.PayosPaymentServiceImpl;
import vn.payos.model.webhooks.ConfirmWebhookResponse;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestHeader;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.SEE_OTHER;
import static org.monostudio.config.Constants.AUTHORITY_CHECKOUT;
import static org.monostudio.config.Constants.PAYOS_CODE_PARAM;
import static org.monostudio.config.Constants.PAYOS_ORDER_CODE_PARAM;
import static org.monostudio.config.Constants.PAYOS_SUCCESS_PARAM;

@RestController
@RequestMapping("/api/public/checkout")
@Tag(name = "Checkout")
public class PublicCheckoutController {
    private final CheckoutService service;
    private final ObjectMapper objectMapper;

    @Autowired
    public PublicCheckoutController(CheckoutService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    /**
     * Save a new transaction, forward request to checkout server, and save the generated token for later validation
     *
     * @param transactionRequest The checkout details
     * @return An object wrapping the URL and token to redirect the user with, towards their payment page.
     * @throws BadInputException       If the input models class contains invalid data
     * @throws PaymentServiceException If an error happens during the payment
     *                                 payment process
     */
    @PostMapping
    @Operation(summary = "Submit cart contents to request an order and begin a checkout")
    @PreAuthorize("hasAuthority('" + AUTHORITY_CHECKOUT + "')")
    public PaymentRedirectionDetailsPojo submitCart(
        @Valid @RequestBody CheckoutStartRequest transactionRequest,
        @RequestHeader(value = "X-Session-Token", required = false) String sessionTokenHeader
    ) throws BadInputException, PaymentServiceException {
        // Support setting session token from header if absent in body
        if ((transactionRequest.getSessionToken() == null || transactionRequest.getSessionToken().isBlank())
            && sessionTokenHeader != null && !sessionTokenHeader.isBlank()) {
            transactionRequest.setSessionToken(sessionTokenHeader);
        }

        if (transactionRequest.getSessionToken() == null || transactionRequest.getSessionToken().isBlank()) {
            throw new BadInputException("A session token is required (either in body or X-Session-Token header)");
        }

        return service.startCheckout(transactionRequest);
    }

    @PostMapping("/initiate")
    @Operation(summary = "Create order and send OTP to customer email")
    @PreAuthorize("hasAuthority('" + AUTHORITY_CHECKOUT + "')")
    public CheckoutOtpInitiateResponse initiateCheckout(
        @Valid @RequestBody CheckoutStartRequest transactionRequest,
        @RequestHeader(value = "X-Session-Token", required = false) String sessionTokenHeader
    ) throws BadInputException {
        if ((transactionRequest.getSessionToken() == null || transactionRequest.getSessionToken().isBlank())
            && sessionTokenHeader != null && !sessionTokenHeader.isBlank()) {
            transactionRequest.setSessionToken(sessionTokenHeader);
        }
        if (transactionRequest.getSessionToken() == null || transactionRequest.getSessionToken().isBlank()) {
            throw new BadInputException("A session token is required (either in body or X-Session-Token header)");
        }
        return service.initiateCheckoutWithOtp(transactionRequest);
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify checkout OTP and start payment")
    @PreAuthorize("hasAuthority('" + AUTHORITY_CHECKOUT + "')")
    public PaymentRedirectionDetailsPojo verifyCheckoutOtp(@Valid @RequestBody CheckoutOtpVerifyRequest request)
        throws BadInputException, PaymentServiceException {
        return service.verifyCheckoutOtp(request.getOrderId(), request.getOtpCode());
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend checkout OTP to checkout form email")
    @PreAuthorize("hasAuthority('" + AUTHORITY_CHECKOUT + "')")
    public CheckoutOtpInitiateResponse resendCheckoutOtp(@Valid @RequestBody CheckoutOtpResendRequest request)
        throws BadInputException {
        return service.resendCheckoutOtp(request.getOrderId(), request.getEmail());
    }

    @GetMapping("/validate")
    @Operation(summary = "Handle PAYOS browser redirect after payment")
    public ResponseEntity<Void> validatePayosRedirect(@RequestParam Map<String, String> transactionData)
        throws BadInputException, EntityNotFoundException, PaymentServiceException {
        String token = resolveToken(transactionData);

        // Production flow: redirect callback is for user navigation only.
        // Payment state is finalized by verified server-to-server webhook POST /validate.
        OrderPojo order = service.getOrderByToken(token);
        if (!"PAYOS".equalsIgnoreCase(order.getPaymentType())) {
            throw new PaymentServiceException("Unsupported checkout payment type: " + order.getPaymentType());
        }

        URI transactionUri = service.generateResultPageUrl(token);
        return ResponseEntity
            .status(SEE_OTHER)
            .location(transactionUri)
            .build();
    }

    @PostMapping("/validate")
    @Operation(summary = "Handle PAYOS server webhook after payment")
    public ResponseEntity<Map<String, Object>> validatePayosWebhook(@RequestBody Map<String, Object> transactionData)
        throws BadInputException, EntityNotFoundException, PaymentServiceException {
        Map<String, String> normalized = normalize(transactionData);
        String token = resolveToken(normalized);

        OrderPojo order = service.getOrderByToken(token);
        PaymentService paymentService = service.getPaymentService(order.getPaymentType());
        if (!"PAYOS".equalsIgnoreCase(order.getPaymentType())) {
            throw new PaymentServiceException("Unsupported checkout payment type: " + order.getPaymentType());
        }
        if (!(paymentService instanceof PayosPaymentServiceImpl payosService)) {
            throw new PaymentServiceException("PAYOS payment service is not available");
        }
        if (!payosService.validateWebhook(transactionData)) {
            throw new PaymentServiceException("Invalid PAYOS webhook signature");
        }

        service.confirmTransaction(token, isAborted(normalized));

        Map<String, Object> acknowledgement = new LinkedHashMap<>();
        acknowledgement.put("code", "00");
        acknowledgement.put("desc", "success");
        return ResponseEntity.ok(acknowledgement);
    }


    @PostMapping("/payos/confirm-webhook")
    @Operation(summary = "Confirm PAYOS webhook URL for current payment channel")
    public ResponseEntity<Map<String, Object>> confirmPayosWebhook(@RequestBody Map<String, String> requestBody)
        throws PaymentServiceException, BadInputException {
        String webhookUrl = requestBody.get("webhookUrl");
        if (webhookUrl == null || webhookUrl.isBlank()) {
            throw new BadInputException("webhookUrl is required");
        }

        PaymentService paymentService = service.getPaymentService("PAYOS");
        if (!(paymentService instanceof PayosPaymentServiceImpl payosService)) {
            throw new PaymentServiceException("PAYOS payment service is not available");
        }

        ConfirmWebhookResponse confirmResult = payosService.confirmWebhook(webhookUrl);
        Map<String, Object> result = objectMapper.convertValue(confirmResult, Map.class);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", result.getOrDefault("code", "00"));
        response.put("desc", result.getOrDefault("desc", "success"));
        response.put("data", result.getOrDefault("data", result));
        return ResponseEntity.ok(response);
    }

    private String resolveToken(Map<String, String> transactionData) throws BadInputException {
        String token = transactionData.get(PAYOS_ORDER_CODE_PARAM);
        if ((token == null || token.isBlank()) && transactionData.get("data") != null) {
            String rawData = transactionData.get("data");
            int index = rawData.indexOf("orderCode=");
            if (index >= 0) {
                int start = index + "orderCode=".length();
                int end = rawData.indexOf(",", start);
                token = end >= 0 ? rawData.substring(start, end).trim() : rawData.substring(start).trim();
            }
        }
        if (token == null || token.isBlank()) {
            throw new BadInputException("No transaction token was provided");
        }
        return token;
    }

    private boolean isAborted(Map<String, String> transactionData) {
        String responseCode = transactionData.get(PAYOS_CODE_PARAM);
        if (responseCode != null && !responseCode.isBlank()) {
            return !"00".equals(responseCode);
        }
        String success = transactionData.get(PAYOS_SUCCESS_PARAM);
        if (success != null && !success.isBlank()) {
            return !"true".equalsIgnoreCase(success);
        }
        String status = transactionData.get("status");
        if (status != null && !status.isBlank()) {
            if ("CANCELLED".equalsIgnoreCase(status)) {
                return true;
            }
            if ("PAID".equalsIgnoreCase(status)) {
                return false;
            }
        }
        return false;
    }

    private Map<String, String> normalize(Map<String, Object> transactionData) {
        Map<String, String> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : transactionData.entrySet()) {
            normalized.put(entry.getKey(), entry.getValue() == null ? null : String.valueOf(entry.getValue()));
        }
        Object data = transactionData.get("data");
        if (data instanceof Map<?, ?> nested) {
            Object orderCode = nested.get(PAYOS_ORDER_CODE_PARAM);
            if (orderCode != null) {
                normalized.put(PAYOS_ORDER_CODE_PARAM, String.valueOf(orderCode));
            }
            Object status = nested.get("status");
            if (status != null) {
                normalized.put("status", String.valueOf(status));
            }
            Object code = nested.get(PAYOS_CODE_PARAM);
            if (code != null) {
                normalized.put(PAYOS_CODE_PARAM, String.valueOf(code));
            }
            Object success = nested.get(PAYOS_SUCCESS_PARAM);
            if (success != null) {
                normalized.put(PAYOS_SUCCESS_PARAM, String.valueOf(success));
            }
        }
        return normalized;
    }

    @ResponseStatus(INTERNAL_SERVER_ERROR)
    @ExceptionHandler(PaymentServiceException.class)
    public String handleException(PaymentServiceException ex) {
        return ex.getMessage();
    }
}
