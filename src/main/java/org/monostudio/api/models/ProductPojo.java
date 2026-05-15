package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Collection;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class ProductPojo {
    private Long id;
    @NotBlank
    private String name;
    @NotBlank
    private String barcode;
    @JsonInclude(NON_EMPTY)
    private String description;
    /**
     * @deprecated Legacy alias for currentPrice. Use currentPrice/originalPrice instead.
     */
    @Deprecated
    @NotNull
    private Integer price;
    /** List/base price before promotional pricing is applied. */
    private Integer originalPrice;
    /** Current effective selling price at request time. */
    private Integer currentPrice;
    /** Rounded percentage discount from originalPrice to currentPrice. */
    private Integer discountPercent;
    /** Whether a promotion is currently active for this product. */
    private Boolean hasDiscount;
    /** Active window start of applied promotion, if any. */
    private LocalDateTime discountActiveFrom;
    /** Active window end of applied promotion, if any. */
    private LocalDateTime discountActiveUntil;
    /**
     * Aggregate stock across active variants.
     * Read-only for variant-based products.
     */
    private Integer currentStock;
    /**
     * Aggregate reserved stock across active variants.
     * Read-only for variant-based products.
     */
    private Integer reservedStock;
    /**
     * Aggregate low-stock threshold across active variants.
     * Read-only for variant-based products.
     */
    private Integer criticalStock;

    public Integer getAvailableStock() {
        if (currentStock == null) return 0;
        return currentStock - (reservedStock != null ? reservedStock : 0);
    }
    private ProductCategoryPojo category;
    private Collection<ImagePojo> images;

    /** URL of the primary image. Read-only convenience field for listings/cart. */
    private String primaryImageUrl;

    // --- Review statistics (populated when including review stats) ---
    /** Average rating across approved reviews (1 decimal place), null if no reviews */
    private Double averageRating;
    /** Total count of approved reviews */
    private Integer totalReviews;

    /**
     * Product visibility lifecycle status.
     * - DRAFT:     not visible to customers (pre-launch)
     * - PUBLISHED: visible and available for purchase
     * - UNLISTED:  was available but now hidden
     */
    private String status;
}
