package org.monostudio.jpa.repositories;

import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.OrderOtp;

import java.time.Instant;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface OrderOtpRepository extends Repository<OrderOtp> {
    Optional<OrderOtp> findTopByOrderIdOrderByCreatedAtDesc(Long orderId);

    Optional<OrderOtp> findTopByOrderIdAndUsedFalseOrderByCreatedAtDesc(Long orderId);

    void deleteByUsedTrueAndVerifiedAtBefore(Instant cutoff);
}
