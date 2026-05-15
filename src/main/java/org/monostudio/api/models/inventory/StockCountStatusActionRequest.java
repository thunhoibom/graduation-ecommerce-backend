package org.monostudio.api.models.inventory;

import lombok.Data;

@Data
public class StockCountStatusActionRequest {
    private Long actorId;
    private String note;
}
