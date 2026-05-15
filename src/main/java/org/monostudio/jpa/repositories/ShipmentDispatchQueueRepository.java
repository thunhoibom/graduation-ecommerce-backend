package org.monostudio.jpa.repositories;

import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.ShipmentDispatchQueue;
import org.monostudio.jpa.entities.ShipmentDispatchQueue.DispatchStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface ShipmentDispatchQueueRepository extends Repository<ShipmentDispatchQueue> {
    @Query("SELECT q FROM ShipmentDispatchQueue q WHERE q.order.id = :orderId")
    Optional<ShipmentDispatchQueue> findByOrderId(@Param("orderId") Long orderId);

    @Query("SELECT q FROM ShipmentDispatchQueue q WHERE q.status = :status AND q.nextRetryAt <= :now")
    List<ShipmentDispatchQueue> findPendingDueForRetry(
        @Param("status") DispatchStatus status,
        @Param("now") Instant now
    );

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ShipmentDispatchQueue q SET q.status = 'RETRYING' WHERE q.id = :id AND q.status = 'PENDING'")
    int markAsRetrying(@Param("id") Long id);
}
