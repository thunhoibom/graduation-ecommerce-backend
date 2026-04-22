package org.monostudio.jpa.repositories;

import java.util.List;
import java.util.Optional;
import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.StockCountSession;
import org.monostudio.jpa.entities.StockCountSession.StockCountStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@org.springframework.stereotype.Repository
public interface StockCountSessionsRepository
    extends Repository<StockCountSession> {

    Optional<StockCountSession> findByCode(String code);

    @Query("""
        SELECT sc FROM StockCountSession sc
        WHERE (:status IS NULL OR sc.status = :status)
        ORDER BY sc.createdAt DESC
        """)
    List<StockCountSession> findByStatus(@Param("status") StockCountStatus status);

    long countByStatus(StockCountStatus status);
}
