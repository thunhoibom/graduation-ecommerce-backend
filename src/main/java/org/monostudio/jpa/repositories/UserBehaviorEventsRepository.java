package org.monostudio.jpa.repositories;

import org.monostudio.jpa.entities.UserBehaviorEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@org.springframework.stereotype.Repository
public interface UserBehaviorEventsRepository extends org.monostudio.jpa.Repository<UserBehaviorEvent> {

    List<UserBehaviorEvent> findByDeviceIdOrderByCreatedAtDesc(String deviceId, Pageable pageable);

    List<UserBehaviorEvent> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    /** PRODUCT_VIEW events whose JSON payload includes {@code productId}. */
    @Query(value = """
        SELECT COUNT(*)
        FROM user_behavior_events
        WHERE event_type = 'PRODUCT_VIEW'
          AND (payload::json->>'productId') IS NOT NULL
          AND (payload::json->>'productId')::bigint = :productId
        """, nativeQuery = true)
    long countProductViewsForProduct(@Param("productId") long productId);
}
