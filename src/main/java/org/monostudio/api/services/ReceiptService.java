package org.monostudio.api.services;

import org.monostudio.api.models.ReceiptPojo;

import jakarta.persistence.EntityNotFoundException;

/**
 * Provides a mean for users to review past orders.
 */
public interface ReceiptService {

    /**
     * Fetches receipt data for a transaction matching a given token.
     *
     * @param token The transaction token.
     * @return The details of the transaction.
     * @throws EntityNotFoundException When no transaction matches the input token
     */
    ReceiptPojo fetchReceiptByTransactionToken(String token) throws EntityNotFoundException;
}
