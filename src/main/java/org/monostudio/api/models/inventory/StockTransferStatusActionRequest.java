package org.monostudio.api.models.inventory;

import lombok.Data;

@Data
public class StockTransferStatusActionRequest {
    private Long actorId;
    private String note;
}
