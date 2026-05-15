package org.monostudio.jpa.repositories;

import org.monostudio.jpa.entities.ReturnRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnRequestsRepository
    extends org.monostudio.jpa.Repository<ReturnRequest> {
    Optional<ReturnRequest> findByOrderId(Long orderId);

    @Query(value = "SELECT * FROM return_requests r ORDER BY r.return_request_date DESC LIMIT :limit", nativeQuery = true)
    List<ReturnRequest> findRecentRequests(@Param("limit") int limit);
}
