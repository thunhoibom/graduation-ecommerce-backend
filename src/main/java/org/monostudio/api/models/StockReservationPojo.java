package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class StockReservationPojo {

    private Long id;

    /** Cart session identifier */
    @NotBlank
    private String sessionId;

    /** SKU of the variant to reserve */
    @NotBlank
    private String variantSku;

    /** SKU — resolved and returned to caller */
    private String variantSkuResolved;

    /** Variant size label */
    private String variantSize;

    /** Variant color label */
    private String variantColor;

    /** Parent product name */
    private String productName;

    /** Parent product barcode */
    private String productBarcode;

    @Min(1)
    @NotNull
    private Integer quantity;

    /** RESERVED | CONFIRMED | RELEASED */
    @Builder.Default
    private String status = "RESERVED";

    /** Expiry timestamp */
    private LocalDateTime expiresAt;

    /** Creation timestamp (read-only) */
    private LocalDateTime createdAt;

    /** Update timestamp (read-only) */
    private LocalDateTime updatedAt;
}
