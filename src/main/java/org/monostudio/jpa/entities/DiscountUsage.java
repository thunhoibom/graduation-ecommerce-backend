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

/**
 * Tracks per-customer usage of discount codes.
 * Each row = one successful redemption of a discount code by a specific customer.
 * Used to enforce per-customer usage limits (maxUsesPerCustomer) on discount codes.
 */
@Entity
@Table(
    name = "discount_usages",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"discount_usage_discount_id", "discount_usage_customer_id"})
    },
    indexes = {
        @Index(columnList = "discount_usage_discount_id"),
        @Index(columnList = "discount_usage_customer_id"),
        @Index(columnList = "discount_usage_date")
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class DiscountUsage
    implements DBEntity {
    private static final long serialVersionUID = 23L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "discount_usage_id", nullable = false)
    private Long id;

    /**
     * The discount code that was used.
     */
    @JoinColumn(name = "discount_usage_discount_id", nullable = false,
        referencedColumnName = "discount_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private DiscountCode discountCode;

    /**
     * The customer who used the code. null for guest checkouts.
     */
    @JoinColumn(name = "discount_usage_customer_id",
        referencedColumnName = "customer_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Customer customer;

    /**
     * The order in which this discount was applied.
     */
    @JoinColumn(name = "discount_usage_order_id",
        referencedColumnName = "order_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Order order;

    /**
     * When this usage occurred.
     */
    @Column(name = "discount_usage_date", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant date;
}
