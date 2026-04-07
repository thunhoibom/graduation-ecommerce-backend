package org.monostudio.jpa.repositories;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.RefundRetryQueue;
import org.monostudio.jpa.entities.RefundRetryQueue.RefundStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface RefundRetryQueueRepository
    extends Repository<RefundRetryQueue> {

    /**
     * Find all entries that are PENDING and due for retry (nextRetryAt <= now).
     */
    @Query("SELECT r FROM RefundRetryQueue r "
        + "WHERE r.status = :status "
        + "AND r.nextRetryAt <= :now")
    List<RefundRetryQueue> findPendingDueForRetry(
        @Param("status") RefundStatus status,
        @Param("now") Instant now);

    /**
     * Mark a queue entry as RETRYING (optimistic lock on status).
     * Returns 0 if another process already picked it up.
     */
    @Modifying
    @Transactional
    @Query("UPDATE RefundRetryQueue r "
        + "SET r.status = 'RETRYING' "
        + "WHERE r.id = :id AND r.status = 'PENDING'")
    int markAsRetrying(@Param("id") Long id);

    /**
     * Find by order ID.
     */
    Optional<RefundRetryQueue> findByOrderId(Long orderId);

    /**
     * Find all FAILED_PERMANENT entries for admin review.
     */
    List<RefundRetryQueue> findByStatus(RefundStatus status);

    /**
     * Count pending retries.
     */
    long countByStatus(RefundStatus status);
}
