package org.monostudio.api.models.inventory;

import java.util.List;
import lombok.Data;

@Data
public class PurchaseOrderCreateRequest {
    private Long supplierId;
    private String warehouseId;
    private String locationCode;
    private String expectedDate;
    private String note;
    private Long requestedBy;
    private List<Line> lines;

    @Data
    public static class Line {
        private Long variantId;
        private Integer orderedQty;
        private Integer unitCost;
        private String note;
    }
}
