package org.monostudio.jpa.repositories;

import org.monostudio.jpa.entities.UserBehaviorEvent;
import org.springframework.data.domain.Pageable;

import java.util.List;

@org.springframework.stereotype.Repository
public interface UserBehaviorEventsRepository extends org.monostudio.jpa.Repository<UserBehaviorEvent> {

    List<UserBehaviorEvent> findByDeviceIdOrderByCreatedAtDesc(String deviceId, Pageable pageable);

    List<UserBehaviorEvent> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);
}
