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
import jakarta.persistence.Version;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "product_variants",
    indexes = {
        @Index(columnList = "product_id"),
        @Index(columnList = "variant_sku", unique = true)
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class ProductVariant
    implements DBEntity {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "variant_id", nullable = false)
    private Long id;

    /**
     * SKU is the identifying property for a ProductVariant.
     * It must be unique across all variants.
     */
    @Size(max = 100)
    @Column(name = "variant_sku", nullable = false, unique = true)
    private String sku;

    /**
     * Human-readable size label (e.g. "S", "M", "L", "XL", "38", etc.)
     */
    @Size(max = 50)
    @Column(name = "variant_size", nullable = false)
    private String size;

    /**
     * Human-readable color label (e.g. "Red", "Navy", "Black", etc.)
     */
    @Size(max = 50)
    @Column(name = "variant_color")
    private String color;

    /**
     * Additional attributes stored as a flat JSON string.
     * e.g. {"material": "cotton", "weight": "200g"}
     */
    @Column(name = "variant_attributes", length = 2000)
    private String attributes;

    /**
     * Price modifier relative to the parent product's price.
     * Can be positive (premium), zero (base), or negative (discount).
     * Final price = product.price + priceModifier
     */
    @Column(name = "variant_price_modifier", nullable = false)
    private int priceModifier;

    /**
     * Current stock level for this specific variant.
     */
    @Column(name = "variant_stock_current", nullable = false)
    private int stockCurrent;

    /**
     * Low-stock threshold for this variant.
     */
    @Column(name = "variant_stock_critical", nullable = false)
    private int stockCritical;

    /**
     * Quantity of stock currently reserved by active carts (pending checkout).
     * Reserved stock is not available for new orders.
     * availableStock = stockCurrent - stockReserved
     */
    @Column(name = "variant_stock_reserved", nullable = false)
    @Builder.Default
    private int stockReserved = 0;

    @Version
    @Column(name = "variant_version")
    private Long version;

    /**
     * Whether this variant is active and available for purchase.
     */
    @Column(name = "variant_active", nullable = false)
    private boolean active;

    /**
     * Optional barcode / UPC for the specific variant.
     */
    @Size(max = 100)
    @Column(name = "variant_barcode", unique = true)
    private String barcode;

    @JoinColumn(name = "product_id", nullable = false, referencedColumnName = "product_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;

    @CreationTimestamp
    @Column(name = "variant_created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Copy-constructor — does NOT copy the product relationship (set to null).
     *
     * @param source The original ProductVariant
     */
    public ProductVariant(ProductVariant source) {
        this.id = source.id;
        this.sku = source.sku;
        this.size = source.size;
        this.color = source.color;
        this.attributes = source.attributes;
        this.priceModifier = source.priceModifier;
        this.stockCurrent = source.stockCurrent;
        this.stockCritical = source.stockCritical;
        this.stockReserved = source.stockReserved;
        this.active = source.active;
        this.barcode = source.barcode;
        this.product = null;
        this.createdAt = source.createdAt;
    }
}
