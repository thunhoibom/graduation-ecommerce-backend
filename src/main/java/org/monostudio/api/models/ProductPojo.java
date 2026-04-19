package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Collection;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class ProductPojo {
    private Long id;
    @NotBlank
    private String name;
    @NotBlank
    private String barcode;
    @JsonInclude(NON_EMPTY)
    private String description;
    @NotNull
    private Integer price;
    private Integer currentStock;
    private Integer criticalStock;
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
