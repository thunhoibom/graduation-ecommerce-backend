package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StockAdjustmentPojo {
    private Long id;
    private Instant date;
    @NotNull
    private Long variantId;
    private String variantSku;
    private String variantSize;
    private String variantColor;
    private String productName;
    @NotNull
    private String reason;
    private int quantityDelta;
    private int stockBefore;
    private int stockAfter;
    private String description;
    private String sessionId;
    private Long orderId;
    private Long returnRequestId;
    private Long performedBy;
}
