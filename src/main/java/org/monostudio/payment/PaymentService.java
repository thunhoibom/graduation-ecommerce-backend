package org.monostudio.payment;

import org.monostudio.api.models.PaymentRedirectionDetailsPojo;
import org.monostudio.api.models.OrderPojo;

/**
 * Interface for requesting and validating payments through an external payment
 */
public interface PaymentService {
    /**
     * Request an external payment service to generate a transaction process for us.
     *
     * @param transaction The details for the transaction.
     * @return The information with which to proceed to a payment page.
     * @throws PaymentServiceException If the payment service is caught under unexpected circumstances.
     */
    PaymentRedirectionDetailsPojo requestNewPaymentPageDetails(OrderPojo transaction) throws PaymentServiceException;

    /**
     * Request the external payment service to report the status of the transaction matching a given token.
     *
     * @param transactionToken The token to match the transaction with.
     * @return The number code that represents the status of that transaction
     * @throws PaymentServiceException If the payment service is caught under unexpected circumstances.
     */
    int requestPaymentResult(String transactionToken) throws PaymentServiceException;

    /**
     * The configured frontend success page URL.
     *
     * @return Said URL.
     */
    String getPaymentResultPageUrl();
}
