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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Represents a single item line in a shopping cart.
 *
 * <p>Each CartItem references a specific ProductVariant and holds the
 * quantity the customer intends to purchase.</p>
 *
 * <p>The CartItem does NOT hold pricing — pricing is computed at checkout
 * from the variant's current priceModifier and the product's base price.</p>
 */
@Entity
@Table(
    name = "cart_items",
    indexes = {
        @Index(columnList = "item_session_id"),
        @Index(columnList = "item_variant_id")
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class CartItem
    implements DBEntity {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id", nullable = false)
    private Long id;

    @JoinColumn(name = "item_session_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private CartSession cartSession;

    @JoinColumn(name = "item_variant_id", nullable = false, referencedColumnName = "variant_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private ProductVariant variant;

    @Column(name = "item_quantity", nullable = false)
    @Builder.Default
    private int quantity = 1;

    @CreationTimestamp
    @Column(name = "item_added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    @UpdateTimestamp
    @Column(name = "item_updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Copy-constructor — does NOT copy cartSession relationship.
     *
     * @param source The original CartItem
     */
    public CartItem(CartItem source) {
        this.id = source.id;
        this.variant = null; // never copy the relationship
        this.quantity = source.quantity;
        this.addedAt = source.addedAt;
        this.updatedAt = source.updatedAt;
    }
}
