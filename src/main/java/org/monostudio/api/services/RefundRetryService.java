package org.monostudio.api.services;

/**
 * Service for managing the refund retry queue.
 *
 * <p>When a refund fails, it is enqueued here for automatic retry with exponential backoff.
 * After all retry attempts are exhausted, an admin alert is sent.</p>
 */
public interface RefundRetryService {

    /**
     * Enqueue a failed refund for automatic retry.
     * Called by OrdersProcessServiceImpl.markAsAdminCancelled() and
     * ReturnRequestServiceImpl.completeRefund() when the payment gateway fails.
     *
     * @param orderId        The order ID
     * @param transactionToken The payment gateway token for the refund
     * @param amount         Amount to refund, in cents
     * @param reason         Reason for refund (e.g. "ADMIN_CANCELLED", "RETURN_COMPLETED")
     */
    void enqueueFailedRefund(Long orderId, String transactionToken, int amount, String reason);

    /**
     * Process all pending retries that are due.
     * Should be called by a scheduled job (e.g. every 5 minutes).
     *
     * @return Number of retries processed
     */
    int processPendingRetries();

    /**
     * Manually retry a specific FAILED_PERMANENT entry.
     * Admin can trigger a manual retry from the dashboard.
     *
     * @param queueId Queue entry ID
     * @throws Exception if the retry fails
     */
    void manualRetry(Long queueId) throws Exception;
}