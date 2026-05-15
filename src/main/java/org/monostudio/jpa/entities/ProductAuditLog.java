package org.monostudio.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.monostudio.jpa.DBEntity;

import java.time.Instant;

@Entity
@Table(
    name = "product_audit_logs",
    indexes = {
        @Index(columnList = "audit_occurred_at"),
        @Index(columnList = "audit_entity_type,audit_entity_id"),
        @Index(columnList = "audit_product_id"),
        @Index(columnList = "audit_variant_id"),
        @Index(columnList = "audit_actor_username"),
        @Index(columnList = "audit_action"),
        @Index(columnList = "audit_correlation_id")
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class ProductAuditLog
    implements DBEntity {
    private static final long serialVersionUID = 1L;

    public enum EntityType {
        PRODUCT,
        VARIANT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id", nullable = false)
    private Long id;

    @CreationTimestamp
    @Column(name = "audit_occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "audit_actor_username", length = 120)
    private String actorUsername;

    @Column(name = "audit_actor_user_id")
    private Long actorUserId;

    @Column(name = "audit_action", nullable = false, length = 80)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(name = "audit_entity_type", nullable = false, length = 20)
    private EntityType entityType;

    @Column(name = "audit_entity_id", nullable = false)
    private Long entityId;

    @Column(name = "audit_product_id")
    private Long productId;

    @Column(name = "audit_variant_id")
    private Long variantId;

    @Column(name = "audit_entity_code", length = 150)
    private String entityCode;

    @Column(name = "audit_summary", length = 600)
    private String summary;

    @Column(name = "audit_before_snapshot", columnDefinition = "TEXT")
    private String beforeSnapshot;

    @Column(name = "audit_after_snapshot", columnDefinition = "TEXT")
    private String afterSnapshot;

    @Column(name = "audit_request_source", length = 50)
    private String requestSource;

    @Column(name = "audit_correlation_id", length = 120)
    private String correlationId;
}
