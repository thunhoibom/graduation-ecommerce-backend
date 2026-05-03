package org.monostudio.mailing;

import org.monostudio.api.models.OrderPojo;
import org.monostudio.api.models.ReturnRequestPojo;
import java.time.Instant;

/**
 * Point of entry for services to send mail to customers and owners alike
 */
public interface MailingService {
    /**
     * Generate and send an e-mail to the customer, regarding an update on their transaction' status.<br/>
     * Should support all transaction stages
     *
     * @param sell The transaction metadata
     * @throws MailingServiceException When any error occurs while interacting with the mail server/service provider
     */
    void notifyOrderStatusToClient(OrderPojo sell) throws MailingServiceException;

    /**
     * Generate and send an e-mail to store owners, regarding an update on a certain transaction' status.<br/>
     * It is not mandatory to support all transaction stages; owners may only need to be aware of some events.
     *
     * @param sell The transaction metadata
     * @throws MailingServiceException When any error occurs while interacting with the mail server/service provider
     */
    void notifyOrderStatusToOwners(OrderPojo sell) throws MailingServiceException;

    /**
     * Send a low-stock alert to store owners when a variant's available stock
     * drops to or below its critical threshold.
     *
     * @param productName  the name of the product
     * @param currentStock the current available stock level
     * @throws MailingServiceException When any error occurs while interacting with the mail server/service provider
     */
    void notifyLowStockAlert(String productName, int currentStock) throws MailingServiceException;

    /**
     * Notify the customer about a change in their return request status.
     *
     * @param request the return request with updated status
     * @throws MailingServiceException When any error occurs while interacting with the mail server/service provider
     */
    void notifyReturnRequestStatusToClient(ReturnRequestPojo request) throws MailingServiceException;

    /**
     * Notify store owners when a new return request is submitted by a customer.
     *
     * @param request the return request details
     * @throws MailingServiceException When any error occurs while interacting with the mail server/service provider
     */
    void notifyReturnRequestToOwners(ReturnRequestPojo request) throws MailingServiceException;

    /**
     * Send checkout OTP for customer confirmation before payment starts.
     */
    void notifyCheckoutOtp(String email, Long orderId, String otpCode, Instant expiresAt) throws MailingServiceException;
}
