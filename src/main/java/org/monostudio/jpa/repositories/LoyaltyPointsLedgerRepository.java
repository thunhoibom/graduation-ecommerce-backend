package org.monostudio.jpa.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.LoyaltyPointsLedger;

import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface LoyaltyPointsLedgerRepository
    extends Repository<LoyaltyPointsLedger> {

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<LoyaltyPointsLedger> findByIdempotencyKey(String idempotencyKey);

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN l.pointsDelta < 0 THEN -l.pointsDelta ELSE 0 END), 0)
        FROM LoyaltyPointsLedger l
        WHERE l.order.id = :orderId
        """)
    int sumReversedPointsByOrderId(@Param("orderId") Long orderId);

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN l.eventType = :eventType THEN -l.pointsDelta ELSE 0 END), 0)
        FROM LoyaltyPointsLedger l
        WHERE l.order.id = :orderId
        """)
    int sumReversedPointsByOrderIdAndEventType(@Param("orderId") Long orderId, @Param("eventType") String eventType);

    Optional<LoyaltyPointsLedger> findFirstByOrderIdAndEventTypeOrderByCreatedAtAsc(Long orderId, String eventType);

    List<LoyaltyPointsLedger> findTop10ByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
