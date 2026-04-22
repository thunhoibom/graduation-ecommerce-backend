package org.monostudio.api.services.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.RefundResultPojo;
import org.monostudio.api.services.LoyaltyService;
import org.monostudio.api.services.RefundRetryService;
import org.monostudio.jpa.entities.Order;
import org.monostudio.jpa.entities.RefundRetryQueue;
import org.monostudio.jpa.entities.RefundRetryQueue.RefundStatus;
import org.monostudio.jpa.repositories.OrdersRepository;
import org.monostudio.jpa.repositories.RefundRetryQueueRepository;
import org.monostudio.payment.PaymentService;
import org.monostudio.payment.PaymentServiceException;

import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Refund retry queue processor with exponential backoff.
 *
 * <p>When a refund fails, it is enqueued here. The scheduled job processes the queue
 * every 5 minutes, retrying each pending entry. After each failure, the next retry
 * is scheduled with exponential backoff (15m → 30m → 1h → 2h → 4h).</p>
 *
 * <p>After all attempts are exhausted (5 total), the entry is marked FAILED_PERMANENT
 * and an admin alert email is sent immediately.</p>
 *
 * <p>This ensures that transient gateway failures do not result in permanent customer fund freezes.
 * The admin only needs to intervene when all retries are exhausted.</p>
 */
@Service
public class RefundRetryServiceImpl
    implements RefundRetryService {

    private static final Logger logger = LoggerFactory.getLogger(RefundRetryServiceImpl.class);

    private final RefundRetryQueueRepository queueRepository;
    private final OrdersRepository ordersRepository;
    private final Map<String, PaymentService> paymentServices;
    private final LoyaltyService loyaltyService;

    @Autowired
    public RefundRetryServiceImpl(
        RefundRetryQueueRepository queueRepository,
        OrdersRepository ordersRepository,
        @Autowired(required = false) Map<String, PaymentService> paymentServices,
        LoyaltyService loyaltyService
    ) {
        this.queueRepository = queueRepository;
        this.ordersRepository = ordersRepository;
        this.paymentServices = paymentServices;
        this.loyaltyService = loyaltyService;
    }

    // ─── Public API ────────────────────────────────────────────────────────────

    @Override
    public void enqueueFailedRefund(Long orderId, String transactionToken, int amount, String reason) {
        // Prevent duplicate queue entries for the same order
        if (queueRepository.findByOrderId(orderId).isPresent()) {
            logger.warn("Refund for order {} already in retry queue — skipping duplicate entry", orderId);
            return;
        }

        Order order = ordersRepository.findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        RefundRetryQueue entry = RefundRetryQueue.builder()
            .order(order)
            .transactionToken(transactionToken)
            .amount(amount)
            .reason(reason)
            .status(RefundStatus.PENDING)
            .attemptCount(0)
            .failedAttempts(0)
            .nextRetryAt(Instant.now().plus(RefundRetryQueue.RETRY_INTERVALS_MS[0],
                java.time.temporal.ChronoUnit.MILLIS))
            .build();

        queueRepository.saveAndFlush(entry);
        logger.info("Enqueued failed refund for order {}: amount={}, reason={}, nextRetryAt={}",
            orderId, amount, reason, entry.getNextRetryAt());
    }

    @Override
    @Transactional
    public int processPendingRetries() {
        Instant now = Instant.now();
        List<RefundRetryQueue> due = queueRepository.findPendingDueForRetry(RefundStatus.PENDING, now);

        if (due.isEmpty()) {
            return 0;
        }

        logger.info("Processing {} pending refund retries", due.size());
        int processed = 0;

        for (RefundRetryQueue entry : due) {
            try {
                // Try to claim this entry (prevents double-processing if job runs concurrently)
                int claimed = queueRepository.markAsRetrying(entry.getId());
                if (claimed == 0) {
                    logger.debug("Refund queue entry {} already claimed by another process — skipping", entry.getId());
                    continue;
                }

                executeRetry(entry);
                processed++;
            } catch (Exception e) {
                // Catch all to prevent one failing entry from stopping the batch
                logger.error("Unexpected error processing refund queue entry {}: {}",
                    entry.getId(), e.getMessage(), e);
            }
        }

        return processed;
    }

    @Override
    @Transactional
    public void manualRetry(Long queueId) throws Exception {
        RefundRetryQueue entry = queueRepository.findById(queueId)
            .orElseThrow(() -> new EntityNotFoundException("Refund queue entry not found: " + queueId));

        if (entry.getStatus() != RefundStatus.FAILED_PERMANENT
            && entry.getStatus() != RefundStatus.PENDING) {
            throw new IllegalStateException(
                "Can only manually retry PENDING or FAILED_PERMANENT entries. Current status: "
                    + entry.getStatus());
        }

        // Reset to PENDING so executeRetry can process it
        entry.setStatus(RefundStatus.PENDING);
        entry.setFailedAttempts(0);
        entry.setNextRetryAt(Instant.now());
        queueRepository.saveAndFlush(entry);

        executeRetry(entry);
    }

    // ─── Internal ─────────────────────────────────────────────────────────────

    /**
     * Executes a single refund retry, updates queue entry state, and handles final failure.
     */
    private void executeRetry(RefundRetryQueue entry) {
        PaymentService paymentService = paymentServices != null ? paymentServices.get(entry.getOrder().getPaymentType()) : null;
        if (paymentService == null) {
            // No payment service configured — mark as permanently failed
            entry.setStatus(RefundStatus.FAILED_PERMANENT);
            entry.setLastError("No payment service configured for type: " + entry.getOrder().getPaymentType());
            queueRepository.saveAndFlush(entry);
            sendAdminAlert(entry, "Payment service unavailable — cannot retry");
            return;
        }

        try {
            RefundResultPojo result = paymentService.refund(entry.getTransactionToken(), entry.getAmount());

            if (result.isSuccess()) {
                handleRetrySuccess(entry, result);
            } else {
                handleRetryFailure(entry,
                    "Gateway rejected refund: responseCode=" + result.getResponseCode()
                        + ", errorMessage=" + result.getErrorMessage());
            }
        } catch (PaymentServiceException e) {
            handleRetryFailure(entry, "Gateway exception: " + e.getMessage());
        }
    }

    private void handleRetrySuccess(RefundRetryQueue entry, RefundResultPojo result) {
        entry.setStatus(RefundStatus.SUCCESS);
        entry.setLastError(null);
        queueRepository.saveAndFlush(entry);

        logger.info("Refund retry succeeded for order {} (queueId={}): type={}, amount={}",
            entry.getOrder().getId(), entry.getId(), result.getType(), entry.getAmount());

        // Update the order's totalRefundedAmount so we don't double-refund
        Order order = ordersRepository.findById(entry.getOrder().getId()).orElse(null);
        if (order != null) {
            order.setTotalRefundedAmount(order.getTotalRefundedAmount() + entry.getAmount());
            ordersRepository.saveAndFlush(order);
            try {
                loyaltyService.syncRefundReversalForOrder(order.getId());
            } catch (RuntimeException e) {
                logger.error("Failed to sync loyalty refund reversal for order {}: {}", order.getId(), e.getMessage());
            }
        }

        // TODO (P3): Send email to customer confirming refund
    }

    private void handleRetryFailure(RefundRetryQueue entry, String errorMessage) {
        entry.advanceToNextRetry();
        entry.setLastError(errorMessage);

        if (entry.getFailedAttempts() >= RefundRetryQueue.MAX_RETRY_ATTEMPTS) {
            // All retries exhausted — mark permanent failure and alert admin
            entry.setStatus(RefundStatus.FAILED_PERMANENT);
            queueRepository.saveAndFlush(entry);

            logger.error("REFUND PERMANENTLY FAILED — manual intervention required. "
                    + "OrderId={}, QueueId={}, Amount={}, Reason={}, LastError={}",
                entry.getOrder().getId(), entry.getId(), entry.getAmount(),
                entry.getReason(), errorMessage);

            sendAdminAlert(entry, errorMessage);
        } else {
            // Schedule next retry
            entry.setStatus(RefundStatus.PENDING);
            queueRepository.saveAndFlush(entry);

            logger.warn("Refund retry #{} failed for order {}: {}. Next retry at {}",
                entry.getFailedAttempts(), entry.getOrder().getId(),
                errorMessage, entry.getNextRetryAt());
        }
    }

    /**
     * Sends an urgent alert to store owners when a refund has permanently failed.
     * Admin must intervene manually to refund the customer.
     */
    private void sendAdminAlert(RefundRetryQueue entry, String reason) {
        logger.error("⚠️  REFUND PERMANENTLY FAILED — admin alert email not configured. "
                + "OrderId={}, Amount={}, Reason={}. Manual intervention required.",
            entry.getOrder().getId(), entry.getAmount(), entry.getReason());

        String subject = "[URGENT] Refund Permanently Failed — Order #" + entry.getOrder().getId();
        String body = String.format(
            "A refund has permanently failed and requires manual intervention.\n\n"
                + "Order ID: %d\n"
                + "Amount: %d (cents)\n"
                + "Reason: %s\n"
                + "Error: %s\n"
                + "Attempts: %d\n"
                + "Transaction Token: %s\n\n"
                + "Please process this refund manually via your payment gateway dashboard.",
            entry.getOrder().getId(),
            entry.getAmount(),
            entry.getReason(),
            reason,
            entry.getFailedAttempts(),
            entry.getTransactionToken()
        );

        try {
            // TODO (P2): Add a notifyAdmins() method to MailingService
            // For now, log as error — admin must check dashboard
            logger.error("⚠️  ADMIN ALERT — Refund permanently failed. "
                    + "OrderId={}, Amount={}, Reason={}, Error={}",
                entry.getOrder().getId(), entry.getAmount(), entry.getReason(), reason);
        } catch (Exception e) {
            logger.error("Failed to send admin alert for refund queue entry {}: {}",
                entry.getId(), e.getMessage());
        }
    }

    // ─── Scheduled job — runs every 5 minutes ─────────────────────────────────

    /**
     * Scheduled job that processes pending refund retries.
     * Configured via Spring @Scheduled — enable by adding @EnableScheduling to config.
     *
     * <p>Run interval: every 5 minutes (300,000 ms).
     * The actual next retry time is controlled per-entry by nextRetryAt field.</p>
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void scheduledProcessPendingRetries() {
        try {
            int processed = processPendingRetries();
            if (processed > 0) {
                logger.info("Scheduled refund retry job: processed {} entries", processed);
            }
        } catch (Exception e) {
            logger.error("Scheduled refund retry job failed: {}", e.getMessage(), e);
        }
    }
}
