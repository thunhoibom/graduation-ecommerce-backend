package org.monostudio.api.models.inventory;

import lombok.Data;

@Data
public class StockCountSessionCreateRequest {
    private String warehouseId;
    private String locationCode;
    private String plannedAt;
    private String note;
    private Long requestedBy;
}
