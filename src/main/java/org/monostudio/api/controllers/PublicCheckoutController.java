package org.monostudio.api.controllers;

import com.querydsl.core.types.Predicate;
import io.jsonwebtoken.lang.Maps;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.monostudio.api.models.CheckoutStartRequest;
import org.monostudio.api.models.PaymentRedirectionDetailsPojo;
import org.monostudio.api.models.OrderPojo;
import org.monostudio.api.services.CheckoutService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.services.crud.OrdersCrudService;
import org.monostudio.jpa.services.predicates.OrdersPredicateService;
import org.monostudio.payment.PaymentServiceException;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.RequestHeader;
import java.net.URI;
import java.util.Map;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.SEE_OTHER;
import static org.monostudio.config.Constants.AUTHORITY_CHECKOUT;
import static org.monostudio.config.Constants.VNPAY_TXN_REF_PARAM;
import static org.monostudio.config.Constants.VNPAY_RESPONSE_CODE_PARAM;

@RestController
@RequestMapping("/api/public/checkout")
@Tag(name = "Checkout")
public class PublicCheckoutController {
    private final CheckoutService service;
    private final OrdersCrudService ordersCrudService;
    private final OrdersPredicateService ordersPredicateService;

    @Autowired
    public PublicCheckoutController(
        CheckoutService service,
        OrdersCrudService ordersCrudService,
        OrdersPredicateService ordersPredicateService
    ) {
        this.service = service;
        this.ordersCrudService = ordersCrudService;
        this.ordersPredicateService = ordersPredicateService;
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
        if (!transactionData.containsKey(VNPAY_TXN_REF_PARAM)) {
            throw new BadInputException("No transaction token was provided");
        }
        String token = transactionData.get(VNPAY_TXN_REF_PARAM);
        String responseCode = transactionData.get(VNPAY_RESPONSE_CODE_PARAM);
        
        boolean isAborted = responseCode == null || !responseCode.equals("00");
        service.confirmTransaction(token, isAborted);
        
        URI transactionUri = service.generateResultPageUrl(token);
        return ResponseEntity
            .status(SEE_OTHER)
            .location(transactionUri)
            .build();
    }

    @ResponseStatus(INTERNAL_SERVER_ERROR)
    @ExceptionHandler(PaymentServiceException.class)
    public String handleException(PaymentServiceException ex) {
        return ex.getMessage();
    }
}
