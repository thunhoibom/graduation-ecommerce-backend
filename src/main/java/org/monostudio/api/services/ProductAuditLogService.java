package org.monostudio.api.services;

import org.monostudio.api.models.DataPagePojo;
import org.monostudio.api.models.ProductAuditLogPojo;
import org.monostudio.jpa.entities.ProductAuditLog;

import java.time.Instant;

public interface ProductAuditLogService {

    void recordBestEffort(AuditEvent event);

    DataPagePojo<ProductAuditLogPojo> search(
        ProductAuditLog.EntityType entityType,
        Long entityId,
        Long productId,
        Long variantId,
        String action,
        String actorUsername,
        Instant fromTime,
        Instant toTime,
        int pageIndex,
        int pageSize
    );

    DataPagePojo<ProductAuditLogPojo> findByProductId(Long productId, int pageIndex, int pageSize);

    DataPagePojo<ProductAuditLogPojo> findByVariantId(Long variantId, int pageIndex, int pageSize);

    record AuditEvent(
        String action,
        ProductAuditLog.EntityType entityType,
        Long entityId,
        Long productId,
        Long variantId,
        String entityCode,
        Object beforeSnapshot,
        Object afterSnapshot,
        String summary,
        String actorUsername,
        String requestSource,
        String correlationId
    ) {}
}
