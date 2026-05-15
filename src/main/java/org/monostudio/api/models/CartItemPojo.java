package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class CartItemPojo {

    private Long id;

    /** Cart session token (not stored in DB — provided by caller) */
    private String sessionToken;

    /** SKU of the variant to add */
    @NotNull
    private String variantSku;

    /** Quantity to add/update */
    @Min(1)
    private int quantity;

    // ─── Resolved fields (populated by converter / service) ───────────────────

    private String variantSkuResolved;
    private String variantSize;
    private String variantColor;
    private Long productId;
    private String productName;
    private String productBarcode;

    /** Base price of the parent product (in cents) */
    private Integer productBasePrice;

    /** Price modifier for this variant (in cents) */
    private Integer priceModifier;

    /** Final unit price = productBasePrice + priceModifier (in cents) */
    private Integer unitPrice;

    /** Line total = unitPrice × quantity (in cents) */
    private Integer lineTotal;

    /** Current available stock for this variant */
    private Integer availableStock;

    /** Whether the requested quantity is in stock */
    private Boolean inStock;

    /** Whether this variant is active */
    private Boolean active;

    /** Primary image URL for the variant (or product fallback). For cart display. */
    private String primaryImageUrl;

    private LocalDateTime addedAt;
    private LocalDateTime updatedAt;
}
