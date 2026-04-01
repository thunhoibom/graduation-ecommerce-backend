package org.monostudio.mailing;

import org.monostudio.api.models.OrderPojo;

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
}
