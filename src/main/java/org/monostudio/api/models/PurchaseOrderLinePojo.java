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
public class PurchaseOrderLinePojo {
    private Long id;
    private Long variantId;
    private String variantSku;
    private String productName;
    private int orderedQty;
    private int receivedQty;
    private Integer unitCost;
    private Long lineTotalAmount;
    private String note;
    private String barcode;
}
