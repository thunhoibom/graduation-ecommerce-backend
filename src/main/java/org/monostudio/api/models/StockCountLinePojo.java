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
public class StockCountLinePojo {
    private Long id;
    private Long variantId;
    private String variantSku;
    private String productName;
    private int expectedQty;
    private Integer countedQty;
    private Integer varianceQty;
    private String reason;
}
