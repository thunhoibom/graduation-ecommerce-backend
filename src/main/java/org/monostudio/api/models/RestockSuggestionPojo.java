package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class RestockSuggestionPojo {
    private Long variantId;
    private String sku;
    private String productName;
    private String size;
    private String color;
    private int currentStock;
    private int criticalStock;
    private int lookbackDays;
    private int leadTimeDays;
    private long soldInLookback;
    private double avgDailySold;
    private int projectedDemand;
    private int safetyStock;
    private int recommendedRestockQty;
}
