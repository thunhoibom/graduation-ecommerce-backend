package org.monostudio.jpa.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.monostudio.jpa.DBEntity;

import java.time.Instant;

/**
 * Queue for tracking GHN return-shipment creation requests.
 *
 * <p>When an admin approves a return request, we immediately try to create a GHN
 * reverse-shipment (Khách → Kho). If the GHN API is unavailable, we enqueue and
 * retry with exponential backoff so no request is silently dropped.</p>
 *
 * <p>Lifecycle:</p>
 * <ol>
 *   <li>{@code PENDING}  — inserted on approve, waiting for first/next attempt</li>
 *   <li>{@code RETRYING} — attempt in progress</li>
 *   <li>{@code SUCCESS}  — GHN accepted the request; tracking number stored</li>
 *   <li>{@code FAILED_PERMANENT} — max retries exhausted; admin must handle manually</li>
 * </ol>
 *
 * <p>Retry schedule: 5m → 15m → 30m → 1h → 2h (same cadence as ShipmentDispatchQueue).</p>
 */
@Entity
@Table(
    name = "return_shipment_dispatch_queue",
    indexes = {
        @Index(columnList = "return_dispatch_status"),
        @Index(columnList = "return_dispatch_next_retry_at"),
        @Index(columnList = "return_dispatch_return_request_id")
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class ReturnShipmentDispatchQueue implements DBEntity {
    private static final long serialVersionUID = 22L;

    public static final int MAX_RETRY_ATTEMPTS = 5;

    /** Milliseconds between retry intervals: 5m, 15m, 30m, 1h, 2h */
    public static final long[] RETRY_INTERVALS_MS = {
        5 * 60 * 1000L,
        15 * 60 * 1000L,
        30 * 60 * 1000L,
        60 * 60 * 1000L,
        2 * 60 * 60 * 1000L
    };

    public enum DispatchStatus {
        PENDING,
        RETRYING,
        SUCCESS,
        FAILED_PERMANENT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_dispatch_id", nullable = false)
    private Long id;

    /** The return request this shipment is for. One-to-one (one dispatch per request). */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_dispatch_return_request_id", nullable = false, unique = true)
    private ReturnRequest returnRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_dispatch_status", nullable = false, length = 32)
    @Builder.Default
    private DispatchStatus status = DispatchStatus.PENDING;

    @Builder.Default
    @Column(name = "return_dispatch_attempt_count", nullable = false)
    private int attemptCount = 0;

    @Builder.Default
    @Column(name = "return_dispatch_failed_attempts", nullable = false)
    private int failedAttempts = 0;

    @Column(name = "return_dispatch_next_retry_at", nullable = false)
    private Instant nextRetryAt;

    @Column(name = "return_dispatch_last_error", length = 1000)
    private String lastError;

    /** The GHN tracking number assigned to this return shipment (set on SUCCESS). */
    @Column(name = "return_dispatch_tracking_number", length = 100)
    private String trackingNumber;

    @CreationTimestamp
    @Column(name = "return_dispatch_created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "return_dispatch_updated_at", nullable = false)
    private Instant updatedAt;

    /** Advance to the next retry window using exponential backoff. */
    public void advanceToNextRetry() {
        failedAttempts++;
        attemptCount++;
        if (failedAttempts > RETRY_INTERVALS_MS.length) {
            status = DispatchStatus.FAILED_PERMANENT;
            return;
        }
        long delay = RETRY_INTERVALS_MS[failedAttempts - 1];
        nextRetryAt = Instant.now().plusMillis(delay);
        status = DispatchStatus.PENDING;
    }
}
