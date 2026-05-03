package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import static org.monostudio.config.Constants.MOMO_ORDER_ID_PARAM;
import static org.monostudio.config.Constants.MOMO_RESULT_CODE_PARAM;
import static org.monostudio.config.Constants.PAYOS_CODE_PARAM;
import static org.monostudio.config.Constants.PAYOS_ORDER_CODE_PARAM;
import static org.monostudio.config.Constants.PAYOS_SUCCESS_PARAM;
import static org.monostudio.config.Constants.VNPAY_TXN_REF_PARAM;
import static org.monostudio.config.Constants.VNPAY_RESPONSE_CODE_PARAM;

@RestController
@RequestMapping("/api/public/checkout")
@Tag(name = "Checkout")
public class PublicCheckoutController {
    private final CheckoutService service;

    @Autowired
    public PublicCheckoutController(CheckoutService service) {
        this.service = service;
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
    @Operation(summary = "Resend checkout OTP to customer email")
    @PreAuthorize("hasAuthority('" + AUTHORITY_CHECKOUT + "')")
    public CheckoutOtpInitiateResponse resendCheckoutOtp(@Valid @RequestBody CheckoutOtpResendRequest request)
        throws BadInputException {
        return service.resendCheckoutOtp(request.getOrderId());
    }

    /**
     * Validate token sent from VNPAY after a transaction is completed or aborted
     *
     * @param transactionData The HTTP parameters
     * @return A 303 SEE OTHER response
     * @throws BadInputException       If the expected token is not present in the request
     * @throws EntityNotFoundException If the token does not match that of any "pending" transaction
     * @throws PaymentServiceException If an error happens during internal API calls
     */
    @GetMapping("/validate")
    @Operation(summary = "Request that an order status be updated after having begun checkout")
    public ResponseEntity<Void> validateTransaction(@RequestParam Map<String, String> transactionData)
        throws BadInputException, EntityNotFoundException, PaymentServiceException {
        String token = resolveToken(transactionData);

        // Resolve payment service to validate callback hash/integrity
        OrderPojo order = service.getOrderByToken(token);
        PaymentService paymentService = service.getPaymentService(order.getPaymentType());
        if (!paymentService.validateCallback(transactionData)) {
            throw new PaymentServiceException("Invalid payment callback security hash");
        }

        boolean isAborted = isAborted(order.getPaymentType(), transactionData);
        service.confirmTransaction(token, isAborted);

        URI transactionUri = service.generateResultPageUrl(token);
        return ResponseEntity
            .status(SEE_OTHER)
            .location(transactionUri)
            .build();
    }

    /**
     * Handles MOMO server-to-server notify URL callbacks (JSON body).
     * Returns acknowledgement response expected by MOMO.
     */
    @PostMapping("/validate")
    @Operation(summary = "Validate payment callback from gateway server-side notification")
    public ResponseEntity<Map<String, Object>> validateTransactionNotify(@RequestBody Map<String, Object> transactionData)
        throws BadInputException, EntityNotFoundException, PaymentServiceException {
        Map<String, String> normalized = normalize(transactionData);
        String token = resolveToken(normalized);

        OrderPojo order = service.getOrderByToken(token);
        PaymentService paymentService = service.getPaymentService(order.getPaymentType());
        if (!paymentService.validateCallback(normalized)) {
            throw new PaymentServiceException("Invalid payment callback security hash");
        }

        boolean isAborted = isAborted(order.getPaymentType(), normalized);
        service.confirmTransaction(token, isAborted);

        Map<String, Object> acknowledgement = new LinkedHashMap<>();
        acknowledgement.put("resultCode", 0);
        acknowledgement.put("message", "OK");
        return ResponseEntity.ok(acknowledgement);
    }

    @PostMapping("/payos/confirm-webhook")
    @Operation(summary = "Confirm PAYOS webhook URL for current payment channel")
    public ConfirmWebhookResponse confirmPayosWebhook(@RequestBody Map<String, String> requestBody)
        throws PaymentServiceException, BadInputException {
        String webhookUrl = requestBody.get("webhookUrl");
        if (webhookUrl == null || webhookUrl.isBlank()) {
            throw new BadInputException("webhookUrl is required");
        }

        PaymentService paymentService = service.getPaymentService("PAYOS");
        if (!(paymentService instanceof PayosPaymentServiceImpl payosService)) {
            throw new PaymentServiceException("PAYOS payment service is not available");
        }
        return payosService.confirmWebhook(webhookUrl);


    // Map<String, Object> response = new LinkedHashMap<>();
    // response.put("code", "00");
    // response.put("desc", "success");
    // return ResponseEntity.ok(response);
    }

    private String resolveToken(Map<String, String> transactionData) throws BadInputException {
        String token = transactionData.get(VNPAY_TXN_REF_PARAM);
        if (token == null || token.isBlank()) {
            token = transactionData.get(MOMO_ORDER_ID_PARAM);
        }
        if (token == null || token.isBlank()) {
            token = transactionData.get(PAYOS_ORDER_CODE_PARAM);
        }
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

    private boolean isAborted(String paymentType, Map<String, String> transactionData) {
        if ("VNPAY".equalsIgnoreCase(paymentType)) {
            String responseCode = transactionData.get(VNPAY_RESPONSE_CODE_PARAM);
            return responseCode == null || !responseCode.equals("00");
        }
        if ("MOMO".equalsIgnoreCase(paymentType)) {
            String resultCode = transactionData.get(MOMO_RESULT_CODE_PARAM);
            return resultCode == null || !resultCode.equals("0");
        }
        if ("PAYOS".equalsIgnoreCase(paymentType)) {
            String responseCode = transactionData.get(PAYOS_CODE_PARAM);
            if (responseCode != null && !responseCode.isBlank()) {
                return !"00".equals(responseCode);
            }
            String success = transactionData.get(PAYOS_SUCCESS_PARAM);
            if (success != null && !success.isBlank()) {
                return !"true".equalsIgnoreCase(success);
            }
            return false;
        }
        String fallbackCode = transactionData.getOrDefault(MOMO_RESULT_CODE_PARAM,
            transactionData.get(VNPAY_RESPONSE_CODE_PARAM));
        return fallbackCode == null || (!fallbackCode.equals("0") && !fallbackCode.equals("00"));
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
        }
        return normalized;
    }

    @ResponseStatus(INTERNAL_SERVER_ERROR)
    @ExceptionHandler(PaymentServiceException.class)
    public String handleException(PaymentServiceException ex) {
        return ex.getMessage();
    }
}
