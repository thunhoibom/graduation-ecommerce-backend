package org.monostudio.jpa.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.monostudio.jpa.DBEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.util.List;

@Entity
@Table(
    name = "products",
    indexes = {
        @Index(columnList = "product_name")
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class Product
    implements DBEntity {
    private static final long serialVersionUID = 10L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id", nullable = false)
    private Long id;
    @Size(max = 200)
    @Column(name = "product_name", nullable = false, unique = true)
    private String name;
    @Size(max = 50)
    @Column(name = "product_code", nullable = false, unique = true)
    private String barcode;
    @Size(max = 4000)
    @Column(name = "product_description")
    private String description;
    @Column(name = "product_price", nullable = false)
    private int price;
    @Column(name = "product_stock_current", nullable = false)
    private int stockCurrent;
    @Column(name = "product_stock_critical", nullable = false)
    private int stockCritical;
    @JoinColumn(name = "product_category_id", referencedColumnName = "product_category_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private ProductCategory productCategory;

    /**
     * All size/color variants for this product.
     * Managed by ProductVariant CRUD — cascade ALL ensures variants are persisted/removed with the product.
     */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<ProductVariant> variants;

    /**
     * All reviews for this product.
     * Cascade REMOVE ensures reviews are deleted when product is deleted.
     */
    @OneToMany(mappedBy = "product", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<ProductReview> reviews;

    /**
     * Product visibility lifecycle status.
     * Controls whether the product appears in public product listings.
     * - DRAFT:     not visible to customers (pre-launch)
     * - PUBLISHED: visible and available for purchase
     * - UNLISTED:  was available but now hidden (discontinued, seasonal, etc.)
     */
    @Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "product_status", nullable = false)
    @Builder.Default
    private ProductStatus status = ProductStatus.DRAFT;

    /**
     * Please note: this copy-constructor does not include a Product's relationship to a ProductCategory,
     * Variants, or Reviews.
     *
     * @param source The original Product
     */
    public Product(Product source) {
        this.id = source.id;
        this.name = source.name;
        this.barcode = source.barcode;
        this.description = source.description;
        this.price = source.price;
        this.stockCurrent = source.stockCurrent;
        this.stockCritical = source.stockCritical;
        this.productCategory = null;
        this.status = source.status;
    }
}
