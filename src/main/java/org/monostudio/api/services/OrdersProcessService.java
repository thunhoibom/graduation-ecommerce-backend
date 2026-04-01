package org.monostudio.api.services;

import org.monostudio.api.models.OrderPojo;
import org.monostudio.common.exceptions.BadInputException;

import jakarta.persistence.EntityNotFoundException;

/**
 * Declares methods to advance through steps of transaction.
 */
public interface OrdersProcessService {

    /**
     * Updates status of a sell to "started", meaning its bill must be paid.
     *
     * @param sell The sell whose status will be updated
     * @throws BadInputException       When the transaction is not in "pending" state, or when it doesn't have a token
     * @throws EntityNotFoundException When the transaction is not found in the persistence context
     */
    OrderPojo markAsStarted(OrderPojo sell) throws BadInputException, EntityNotFoundException;

    /**
     * Updates status of a sell to "aborted", meaning nothing more can be done about it.
     *
     * @param sell The sell whose status will be updated
     * @throws BadInputException       When the transaction is not in "started" state
     * @throws EntityNotFoundException When the transaction is not found in the persistence context
     */
    OrderPojo markAsAborted(OrderPojo sell) throws BadInputException, EntityNotFoundException;

    /**
     * Updates status of a sell to "failed", meaning nothing more can be done about it.
     *
     * @param sell The sell whose status will be updated
     * @throws BadInputException       When the transaction is not in "started" state
     * @throws EntityNotFoundException When the transaction is not found in the persistence context
     */
    OrderPojo markAsFailed(OrderPojo sell) throws BadInputException, EntityNotFoundException;

    /**
     * Updates status of a sell to "paid/unconfirmed", meaning it can be "rejected" or "confirmed" afterwards.
     * This must be only possible right after the sell has been created.
     *
     * @param sell The sell whose status will be updated
     * @throws BadInputException       When the transaction is not in "started" state
     * @throws EntityNotFoundException When the transaction is not found in the persistence context
     */
    OrderPojo markAsPaid(OrderPojo sell) throws BadInputException, EntityNotFoundException;

    /**
     * Updates status of a sell to "confirmed", meaning it must be eventually delivered, or fail to do so.
     *
     * @param sell The sell whose status will be updated
     * @throws BadInputException       When the transaction is not in "paid/unconfirmed" state
     * @throws EntityNotFoundException When the transaction is not found in the persistence context
     */
    OrderPojo markAsConfirmed(OrderPojo sell) throws BadInputException, EntityNotFoundException;

    /**
     * Updates status of a sell to "rejected", meaning a refund must be issued to the customer.
     * This must be only possible after payment is completed, notified and saved in the application state.
     *
     * @throws BadInputException       When the transaction is not "paid/unconfirmed" state
     * @throws EntityNotFoundException When the transaction is not found in the persistence context
     */
    OrderPojo markAsRejected(OrderPojo sell) throws BadInputException, EntityNotFoundException;

    /**
     * Updates status of a sell to "completed", meaning the whole process is finished and the customer is contempt.
     *
     * @param sell The sell whose status will be updated
     * @throws BadInputException       When the transaction is not in "confirmed" state
     * @throws EntityNotFoundException When the transaction is not found in the persistence context
     */
    OrderPojo markAsCompleted(OrderPojo sell) throws BadInputException, EntityNotFoundException;
}
