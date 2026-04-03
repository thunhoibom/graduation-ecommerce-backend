package org.monostudio.jpa.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.monostudio.jpa.DBEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a discount code / coupon that can be applied at checkout.
 *
 * <p>Supported types:</p>
 * <ul>
 *   <li>{@code PERCENTAGE} — percentage off the cart subtotal (0.01 – 100.00)</li>
 *   <li>{@code FIXED_AMOUNT} — fixed monetary discount (in cents)</li>
 *   <li>{@code FREE_SHIPPING} — frees the shipping cost</li>
 * </ul>
 *
 * <p>Usage limits:</p>
 * <ul>
 *   <li>Per-code max uses (total)</li>
 *   <li>Per-customer max uses</li>
 *   <li>Validity window (validFrom – validUntil)</li>
 *   <li>Minimum cart value</li>
 * </ul>
 */
@Entity
@Table(
    name = "discount_codes",
    indexes = {
        @Index(columnList = "discount_code", unique = true),
        @Index(columnList = "discount_active")
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class DiscountCode
    implements DBEntity {
    private static final long serialVersionUID = 1L;

    public static final String TYPE_PERCENTAGE   = "PERCENTAGE";
    public static final String TYPE_FIXED_AMOUNT = "FIXED_AMOUNT";
    public static final String TYPE_FREE_SHIPPING = "FREE_SHIPPING";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "discount_id", nullable = false)
    private Long id;

    /**
     * The code as typed by the customer (e.g. "SUMMER20").
     * Case-insensitive uniqueness.
     */
    @Size(max = 50)
    @Column(name = "discount_code", nullable = false, unique = true)
    private String code;

    /**
     * Human-readable description shown at checkout.
     */
    @Size(max = 200)
    @Column(name = "discount_description")
    private String description;

    /**
     * Discount type: PERCENTAGE | FIXED_AMOUNT | FREE_SHIPPING
     */
    @Column(name = "discount_type", nullable = false, length = 20)
    private String type;

    /**
     * For PERCENTAGE: value from 0.01 to 100.00.
     * For FIXED_AMOUNT: value in cents (e.g. 2000 = $20.00 off).
     * Ignored for FREE_SHIPPING.
     */
    @Column(name = "discount_value", nullable = false)
    private int value;

    /**
     * Maximum number of times this code can be used in total.
     * null = unlimited.
     */
    @Column(name = "discount_max_uses")
    private Integer maxUses;

    /**
     * Current total redemption count.
     */
    @Column(name = "discount_use_count", nullable = false)
    @Builder.Default
    private int useCount = 0;

    /**
     * Maximum uses per individual customer.
     * null = unlimited.
     */
    @Column(name = "discount_max_uses_per_customer")
    private Integer maxUsesPerCustomer;

    /**
     * Minimum cart subtotal required to apply this code (in cents).
     * null = no minimum.
     */
    @Column(name = "discount_min_cart_value")
    private Integer minCartValue;

    /**
     * Start of validity window.
     */
    @Column(name = "discount_valid_from")
    private LocalDateTime validFrom;

    /**
     * End of validity window.
     */
    @Column(name = "discount_valid_until")
    private LocalDateTime validUntil;

    /**
     * Whether this code is currently active and can be applied.
     */
    @Column(name = "discount_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "discount_created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "discount_updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Copy-constructor.
     *
     * @param source The original DiscountCode
     */
    public DiscountCode(DiscountCode source) {
        this.id = source.id;
        this.code = source.code;
        this.description = source.description;
        this.type = source.type;
        this.value = source.value;
        this.maxUses = source.maxUses;
        this.useCount = source.useCount;
        this.maxUsesPerCustomer = source.maxUsesPerCustomer;
        this.minCartValue = source.minCartValue;
        this.validFrom = source.validFrom;
        this.validUntil = source.validUntil;
        this.active = source.active;
        this.createdAt = source.createdAt;
        this.updatedAt = source.updatedAt;
    }
}
