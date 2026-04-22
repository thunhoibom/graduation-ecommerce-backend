package org.monostudio.api.models.inventory;

import java.util.List;
import lombok.Data;

@Data
public class StockCountLineUpsertRequest {
    private List<Line> lines;

    @Data
    public static class Line {
        private Long variantId;
        private Integer countedQty;
        private String reason;
    }
}
