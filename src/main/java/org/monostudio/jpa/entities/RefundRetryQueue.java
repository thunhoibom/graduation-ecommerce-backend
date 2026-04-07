package org.monostudio.jpa.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.monostudio.jpa.DBEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Queue for tracking refund operations that failed and need to be retried.
 *
 * <p>Lifecycle:</p>
 * <ol>
 *   <li>{@code PENDING} — inserted when a refund fails, waiting for next retry window</li>
 *   <li>{@code RETRYING} — retry attempt in progress</li>
 *   <li>{@code SUCCESS} — refund succeeded on retry</li>
 *   <li>{@code FAILED_PERMANENT} — max retry attempts exhausted, admin must handle manually</li>
 * </ol>
 *
 * <p>Retry schedule: 15m → 30m → 1h → 2h → 4h (exponential backoff, 5 attempts max).</p>
 */
@Entity
@Table(
    name = "refund_retry_queue",
    indexes = {
        @Index(columnList = "refund_retry_status"),
        @Index(columnList = "refund_retry_next_retry_at"),
        @Index(columnList = "refund_retry_order_id")
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class RefundRetryQueue
    implements DBEntity {
    private static final long serialVersionUID = 1L;

    public static final int MAX_RETRY_ATTEMPTS = 5;

    /** Milliseconds between retry intervals: 15m, 30m, 1h, 2h, 4h */
    public static final long[] RETRY_INTERVALS_MS = {
        15 * 60 * 1000L,   // attempt 1 → retry in 15 min
        30 * 60 * 1000L,   // attempt 2 → retry in 30 min
        60 * 60 * 1000L,   // attempt 3 → retry in 1 hour
        2 * 60 * 60 * 1000L, // attempt 4 → retry in 2 hours
        4 * 60 * 60 * 1000L  // attempt 5 → retry in 4 hours
    };

    public enum RefundStatus {
        /** Awaiting retry — scheduled at nextRetryAt */
        PENDING,
        /** Retry in progress */
        RETRYING,
        /** Refund succeeded after retry */
        SUCCESS,
        /** All retry attempts exhausted — admin intervention required */
        FAILED_PERMANENT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refund_retry_id", nullable = false)
    private Long id;

    /**
     * The order this refund is for.
     */
    @JoinColumn(name = "refund_retry_order_id", nullable = false,
        referencedColumnName = "order_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Order order;

    /**
     * Payment gateway transaction token used for the refund call.
     */
    @Column(name = "refund_retry_transaction_token", nullable = false, length = 128)
    private String transactionToken;

    /**
     * Amount to refund, in cents.
     */
    @Column(name = "refund_retry_amount", nullable = false)
    private int amount;

    /**
     * How this refund was triggered.
     */
    @Column(name = "refund_retry_reason", nullable = false, length = 64)
    private String reason;

    /**
     * Current status of the retry queue entry.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "refund_retry_status", nullable = false, length = 20)
    @Builder.Default
    private RefundStatus status = RefundStatus.PENDING;

    /**
     * Number of retry attempts made so far (includes the initial failed attempt as attempt 0).
     */
    @Column(name = "refund_retry_attempt_count", nullable = false)
    @Builder.Default
    private int attemptCount = 0;

    /**
     * Timestamp when the next retry attempt should be made.
     */
    @Column(name = "refund_retry_next_retry_at", nullable = false)
    private Instant nextRetryAt;

    /**
     * Timestamp of the last error message from the gateway.
     */
    @Column(name = "refund_retry_last_error", length = 1000)
    private String lastError;

    /**
     * Number of consecutive failed attempts.
     */
    @Column(name = "refund_retry_failed_attempts")
    @Builder.Default
    private int failedAttempts = 0;

    @CreationTimestamp
    @Column(name = "refund_retry_created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "refund_retry_updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Returns the retry interval in milliseconds for the given attempt count.
     * If attempt count exceeds MAX_RETRY_ATTEMPTS, returns the last interval.
     */
    public long getRetryIntervalMs() {
        int idx = Math.min(attemptCount, RETRY_INTERVALS_MS.length - 1);
        return RETRY_INTERVALS_MS[idx];
    }

    /**
     * Advances to next retry window based on exponential backoff.
     * Should be called after a failed retry attempt.
     */
    public void advanceToNextRetry() {
        this.failedAttempts++;
        this.attemptCount++;
        this.nextRetryAt = Instant.now().plus(getRetryIntervalMs(), java.time.temporal.ChronoUnit.MILLIS);
    }
}