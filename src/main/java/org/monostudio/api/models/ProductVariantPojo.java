package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class ProductVariantPojo {

    private Long id;

    @NotBlank
    private String sku;

    @NotBlank
    private String size;

    private String color;

    /** JSON string of additional attributes, e.g. {"material":"cotton"} */
    private String attributes;

    /** Price modifier relative to parent product price. Default 0. */
    @Builder.Default
    private Integer priceModifier = 0;

    private Integer currentStock;

    private Integer criticalStock;

    /**
     * Quantity currently reserved by active carts.
     * Read-only — managed by the Stock Reservation system.
     */
    private Integer reservedStock;

    /**
     * Computed available stock = currentStock - reservedStock.
     * Read-only — derived field, never sent in requests.
     */
    private Integer availableStock;

    @Builder.Default
    private Boolean active = true;

    private String barcode;

    /** Reference to the parent product's barcode. Used for lookups. */
    @NotBlank
    private String productBarcode;

    /** Read-only: resolved parent product name (populated by converter). */
    private String productName;

    /** Read-only: base price of parent product (populated by converter). */
    private Integer productBasePrice;

    /** Read-only: final price = productBasePrice + priceModifier (computed by service). */
    private Integer finalPrice;

    /** Read-only: creation timestamp. */
    private LocalDateTime createdAt;
}
