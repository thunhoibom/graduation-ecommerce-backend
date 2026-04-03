package org.monostudio.api.models;

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
public class ReturnRequestItemPojo {
    private Long id;
    @NotNull
    @Min(1)
    private int quantity;
    private String reason;
    private boolean isActive;
    private Long productId;
    private ProductPojo product;
}
