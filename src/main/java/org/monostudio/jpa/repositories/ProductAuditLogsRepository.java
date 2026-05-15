package org.monostudio.jpa.repositories;

import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.ProductAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

@org.springframework.stereotype.Repository
public interface ProductAuditLogsRepository
    extends Repository<ProductAuditLog> {

    @Query("""
        SELECT l
        FROM ProductAuditLog l
        WHERE (:entityType IS NULL OR l.entityType = :entityType)
          AND (:entityId IS NULL OR l.entityId = :entityId)
          AND (:productId IS NULL OR l.productId = :productId)
          AND (:variantId IS NULL OR l.variantId = :variantId)
          AND (:action IS NULL OR l.action = :action)
          AND (:actorUsername IS NULL OR l.actorUsername = :actorUsername)
          AND l.occurredAt >= COALESCE(:fromTime, l.occurredAt)
          AND l.occurredAt <= COALESCE(:toTime, l.occurredAt)
        """)
    Page<ProductAuditLog> search(
        @Param("entityType") ProductAuditLog.EntityType entityType,
        @Param("entityId") Long entityId,
        @Param("productId") Long productId,
        @Param("variantId") Long variantId,
        @Param("action") String action,
        @Param("actorUsername") String actorUsername,
        @Param("fromTime") Instant fromTime,
        @Param("toTime") Instant toTime,
        Pageable pageable
    );

    Page<ProductAuditLog> findByProductId(Long productId, Pageable pageable);

    Page<ProductAuditLog> findByVariantId(Long variantId, Pageable pageable);
}
