package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * A ProductVariant that has fallen to or below its critical stock threshold.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class LowStockAlertPojo {
    private Long variantId;
    private String productName;
    private String size;
    private String color;
    /** Current stock level for this variant. */
    private int currentStock;
    /** The threshold at which this variant is considered low-stock. */
    private int criticalStock;
}
