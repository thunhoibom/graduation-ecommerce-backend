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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * A product review written by a verified purchaser (Customer).
 */
@Entity
@Table(
    name = "product_reviews",
    indexes = {
        @Index(columnList = "product_id"),
        @Index(columnList = "customer_id"),
        @Index(columnList = "review_approved")
    })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class ProductReview
    implements DBEntity {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id", nullable = false)
    private Long id;

    /**
     * Numeric rating from 1 to 5.
     */
    @Min(1)
    @Max(5)
    @Column(name = "review_rating", nullable = false)
    private int rating;

    /**
     * Optional short title for the review.
     */
    @Size(max = 200)
    @Column(name = "review_title")
    private String title;

    /**
     * The written review body.
     */
    @Size(max = 4000)
    @Column(name = "review_body", length = 4000)
    private String body;

    /**
     * Whether this review has been approved by an admin for public display.
     */
    @Column(name = "review_approved", nullable = false)
    @Builder.Default
    private boolean approved = false;

    /**
     * Whether the reviewer verified their purchase.
     * Set to true automatically when the reviewer has a completed order containing this product.
     */
    @Column(name = "review_verified_purchase", nullable = false)
    @Builder.Default
    private boolean verifiedPurchase = false;

    @JoinColumn(name = "product_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Product product;

    @JoinColumn(name = "customer_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Customer customer;

    @CreationTimestamp
    @Column(name = "review_created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "review_updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Copy-constructor — does NOT copy Product or Customer relationship (set to null).
     *
     * @param source The original ProductReview
     */
    public ProductReview(ProductReview source) {
        this.id = source.id;
        this.rating = source.rating;
        this.title = source.title;
        this.body = source.body;
        this.approved = source.approved;
        this.verifiedPurchase = source.verifiedPurchase;
        this.product = null;
        this.customer = null;
        this.createdAt = source.createdAt;
        this.updatedAt = source.updatedAt;
    }
}
