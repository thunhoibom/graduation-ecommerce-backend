package org.monostudio.jpa.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.monostudio.jpa.DBEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
    name = "loyalty_points_ledger",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"loyalty_event_idempotency_key"})
    },
    indexes = {
        @Index(columnList = "loyalty_customer_id"),
        @Index(columnList = "loyalty_order_id"),
        @Index(columnList = "loyalty_event_type"),
        @Index(columnList = "loyalty_event_created_at")
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class LoyaltyPointsLedger
    implements DBEntity {
    private static final long serialVersionUID = 24L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "loyalty_event_id", nullable = false)
    private Long id;

    @JoinColumn(name = "loyalty_customer_id", nullable = false, referencedColumnName = "customer_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Customer customer;

    @JoinColumn(name = "loyalty_order_id", referencedColumnName = "order_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Order order;

    @Column(name = "loyalty_event_type", nullable = false, length = 48)
    private String eventType;

    @Column(name = "loyalty_points_delta", nullable = false)
    private int pointsDelta;

    @Column(name = "loyalty_source_amount", nullable = false)
    private int sourceAmount;

    @Column(name = "loyalty_event_idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @CreationTimestamp
    @Column(name = "loyalty_event_created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
