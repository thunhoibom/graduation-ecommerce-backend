package org.monostudio.jpa.repositories;

import org.monostudio.jpa.entities.ShipmentTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.Instant;

@Repository
public interface ShipmentTrackingRepository extends JpaRepository<ShipmentTracking, Long> {
    List<ShipmentTracking> findByOrderIdOrderByEventTimeDesc(Long orderId);
    List<ShipmentTracking> findByTrackingNumberOrderByEventTimeDesc(String trackingNumber);
    boolean existsByOrderIdAndTrackingNumberAndStatusAndEventTime(
        Long orderId,
        String trackingNumber,
        String status,
        Instant eventTime
    );
}
