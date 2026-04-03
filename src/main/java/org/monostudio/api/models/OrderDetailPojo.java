package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude
public class OrderDetailPojo {
    private Long id;
    @Min(1)
    private int units;
    private int unitValue;
    private String description;
    @NotNull
    private ProductPojo product;
    /**
     * Optional reference to the ProductVariant used for this line item.
     * Null if this line item was ordered without a variant.
     */
    private Long variantId;
}
