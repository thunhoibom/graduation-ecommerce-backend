package org.monostudio.jpa.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.monostudio.jpa.DBEntity;

import java.time.Instant;

@Entity
@Table(name = "shipment_dispatch_queue")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class ShipmentDispatchQueue implements DBEntity {
    public static final int MAX_RETRY_ATTEMPTS = 5;
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
    @Column(name = "shipment_dispatch_id", nullable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "dispatch_status", nullable = false, length = 32)
    private DispatchStatus status;

    @Builder.Default
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Builder.Default
    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;

    @Column(name = "next_retry_at", nullable = false)
    private Instant nextRetryAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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
