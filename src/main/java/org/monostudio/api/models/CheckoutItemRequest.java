package org.monostudio.api.models;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single line item in a CheckoutStartRequest.
 * References a ProductVariant (preferred) or falls back to a Product by barcode.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutItemRequest {
    /** The product variant ID (preferred). If null, productId is used. */
    private Long variantId;

    /** The product ID (fallback if variantId is null). */
    private Long productId;

    /**
     * Product barcode used for price/unit-value lookup when variantId is null.
     * When variantId is set, this field is optional.
     */
    private String productBarcode;

    @NotNull
    @Min(1)
    private Integer units;

    /**
     * Override unit price. If null, the price is resolved from the variant or product.
     */
    private Integer unitPriceOverride;
}
