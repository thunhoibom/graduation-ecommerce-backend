package org.monostudio.jpa.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
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

@Entity
@Table(
    name = "product_images",
    indexes = {
        @Index(columnList = "product_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"product_id", "image_id"})
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class ProductImage
    implements DBEntity {
    private static final long serialVersionUID = 12L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_image_id", nullable = false)
    private Long id;
    @JoinColumn(name = "image_id", referencedColumnName = "image_id", updatable = false)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Image image;
    @JoinColumn(name = "product_id", referencedColumnName = "product_id", updatable = false)
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Product product;

    /**
     * Display order for this image within the product's image gallery.
     * Lower numbers appear first. Default is 0.
     */
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    /**
     * Whether this image is the primary/hero image for the product.
     * Used as thumbnail in listings, cart, emails, etc.
     * Only one image per product should be marked as primary.
     */
    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    /**
     * Please note: this copy-constructor does NOT include a ProductImage's relationships
     *
     * @param source The original ProductImage
     */
    public ProductImage(ProductImage source) {
        this.id = source.id;
        this.image = null;
        this.product = null;
        this.sortOrder = source.sortOrder;
        this.isPrimary = source.isPrimary;
    }
}
