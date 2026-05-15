package org.monostudio.api.services;

import org.monostudio.api.models.CheckoutStartRequest;
import org.monostudio.api.models.CheckoutOtpInitiateResponse;
import org.monostudio.api.models.PaymentRedirectionDetailsPojo;
import org.monostudio.api.models.OrderPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.payment.PaymentServiceException;

import jakarta.persistence.EntityNotFoundException;
import java.net.URI;

/**
 * Core component of the client-side commerce process.<br/>
 * Helps consumers of the REST API to pay for their products; validate those payments; and finally
 * redirect them to a result page.
 */
public interface CheckoutService {

    /**
     * Full checkout start: validates cart items against available stock, reserves stock,
     * computes shipping fee, applies any discount, creates the order in PENDING status,
     * and returns a payment URL (Webpay Plus).
     *
     * @param request The checkout start request containing cart items, shipping method, etc.
     * @return Payment redirection details with URL and token
     * @throws BadInputException        if cart data, shipping method, or discount code is invalid
     * @throws PaymentServiceException  if the payment gateway fails
     */
    PaymentRedirectionDetailsPojo startCheckout(CheckoutStartRequest request)
        throws BadInputException, PaymentServiceException;

    CheckoutOtpInitiateResponse initiateCheckoutWithOtp(CheckoutStartRequest request)
        throws BadInputException;

    PaymentRedirectionDetailsPojo verifyCheckoutOtp(Long orderId, String otpCode)
        throws BadInputException, PaymentServiceException;

    CheckoutOtpInitiateResponse resendCheckoutOtp(Long orderId, String checkoutEmail) throws BadInputException;

    /**
     * Fetch details to redirect the requester to the payment page; mark transaction as "started";
     * save metadata required for later confirmation
     *
     * @param transaction The "acknowledged" transaction
     * @return Details used by the requester to navigate to the payment page
     * @throws PaymentServiceException On unexpected failures
     */
    PaymentRedirectionDetailsPojo requestTransactionStart(OrderPojo transaction) throws PaymentServiceException, BadInputException;

    /**
     * From a given token, assert existence of a transaction marked as "started"; fetch result of said transaction;
     * update saved metadata of that transaction<br/>
     * Usually, after this the client and the salesmanager are notified by some contact means, such as e-mail
     *
     * @param token      Previously emitted by the payment service
     * @param wasAborted Whether the transaction was aborted by the user doing the payment.
     * @return The "completed/failed" URI for requesting it later on
     * @throws EntityNotFoundException When no transaction matches the provided hash
     * @throws PaymentServiceException On unexpected failures
     */
    OrderPojo confirmTransaction(String token, boolean wasAborted) throws EntityNotFoundException, PaymentServiceException;

    /**
     * From a given token, generate a corresponding URL to redirect users to view their receipt
     *
     * @param transactionToken Previously emitted by the payment service
     * @return The "completed/failed" URI to redirect consumer to
     */
    URI generateResultPageUrl(String transactionToken);
    /**
     * From a given token, retrieve the corresponding order.
     */
    OrderPojo getOrderByToken(String token) throws EntityNotFoundException;

    /**
     * Get the payment service for a given payment type key.
     */
    org.monostudio.payment.PaymentService getPaymentService(String paymentType);
}
