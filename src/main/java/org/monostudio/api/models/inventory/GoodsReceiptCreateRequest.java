package org.monostudio.api.models.inventory;

import java.util.List;
import lombok.Data;

@Data
public class GoodsReceiptCreateRequest {
    private Long receivedBy;
    private String note;
    private List<Line> lines;

    @Data
    public static class Line {
        private Long purchaseOrderLineId;
        private Integer receivedQty;
    }
}
