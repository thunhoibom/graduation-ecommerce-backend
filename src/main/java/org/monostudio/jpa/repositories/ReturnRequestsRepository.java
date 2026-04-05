package org.monostudio.jpa.repositories;

import org.monostudio.jpa.entities.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReturnRequestsRepository
    extends org.monostudio.jpa.Repository<ReturnRequest> {
    Optional<ReturnRequest> findByOrderId(Long orderId);
}
