package org.monostudio.jpa.repositories;

import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.ReturnShipmentDispatchQueue;
import org.monostudio.jpa.entities.ReturnShipmentDispatchQueue.DispatchStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface ReturnShipmentDispatchQueueRepository extends Repository<ReturnShipmentDispatchQueue> {

    ReturnShipmentDispatchQueue saveAndFlush(ReturnShipmentDispatchQueue entity);

    Optional<ReturnShipmentDispatchQueue> findById(Long id);

    @Query("SELECT q FROM ReturnShipmentDispatchQueue q WHERE q.returnRequest.id = :returnRequestId")
    Optional<ReturnShipmentDispatchQueue> findByReturnRequestId(@Param("returnRequestId") Long returnRequestId);

    @Query("SELECT q FROM ReturnShipmentDispatchQueue q "
        + "WHERE q.status = :status AND q.nextRetryAt <= :now "
        + "ORDER BY q.nextRetryAt ASC")
    List<ReturnShipmentDispatchQueue> findPendingDueForRetry(
        @Param("status") DispatchStatus status,
        @Param("now") Instant now
    );

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ReturnShipmentDispatchQueue q "
        + "SET q.status = 'RETRYING' "
        + "WHERE q.id = :id AND q.status = 'PENDING'")
    int markAsRetrying(@Param("id") Long id);

    List<ReturnShipmentDispatchQueue> findByStatus(DispatchStatus status);
}
