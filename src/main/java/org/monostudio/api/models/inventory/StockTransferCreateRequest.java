package org.monostudio.api.models.inventory;

import java.util.List;
import lombok.Data;

@Data
public class StockTransferCreateRequest {
    private String warehouseId;
    private String fromLocation;
    private String toLocation;
    private String note;
    private Long requestedBy;
    private List<Line> lines;

    @Data
    public static class Line {
        private Long variantId;
        private Integer quantity;
    }
}
