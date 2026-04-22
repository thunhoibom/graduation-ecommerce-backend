package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.monostudio.jpa.entities.ProductAuditLog;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductAuditLogPojo {
    private Long id;
    private Instant occurredAt;
    private String actorUsername;
    private Long actorUserId;
    private String action;
    private ProductAuditLog.EntityType entityType;
    private Long entityId;
    private Long productId;
    private Long variantId;
    private String entityCode;
    private String summary;
    private String beforeSnapshot;
    private String afterSnapshot;
    private String requestSource;
    private String correlationId;
}
