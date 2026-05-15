package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class ProductReviewPojo {

    private Long id;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    private String title;

    private String body;

    /** Read-only: whether the review has been approved by an admin */
    private Boolean approved;

    /** Read-only: whether the reviewer has a verified purchase */
    private Boolean verifiedPurchase;

    /** Product barcode — used for lookups when submitting a review */
    @NotBlank
    private String productBarcode;

    /** Read-only: resolved product name */
    private String productName;

    /** Read-only: reviewer's full name */
    private String reviewerName;

    /** Read-only: media associated with this review */
    private List<String> imageUrls;

    /** Write-only: image IDs for new review submission */
    private List<Long> imageIds;

    /** Threaded conversations on this review */
    private List<ProductReviewReplyPojo> replies;

    /** Read-only: creation timestamp */
    private LocalDateTime createdAt;

    /** Read-only: last update timestamp */
    private LocalDateTime updatedAt;
}
