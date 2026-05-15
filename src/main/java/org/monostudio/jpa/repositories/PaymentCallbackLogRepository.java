package org.monostudio.jpa.repositories;

import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.PaymentCallbackLog;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface PaymentCallbackLogRepository
    extends Repository<PaymentCallbackLog> {

    /**
     * Check if a callback with this token has already been processed.
     * Used for idempotency: if token exists, skip reprocessing.
     */
    Optional<PaymentCallbackLog> findByToken(String token);

    /**
     * Check existence — more efficient than findByToken when you only need a boolean.
     */
    boolean existsByToken(String token);

    List<PaymentCallbackLog> findByTokenIn(Collection<String> tokens);

    List<PaymentCallbackLog> findAllByOrderByProcessedAtDesc(Pageable pageable);
}
