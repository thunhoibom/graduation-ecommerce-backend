package org.monostudio.api.models.inventory;

import lombok.Data;

@Data
public class PurchaseOrderStatusActionRequest {
    private Long actorId;
    private String note;
}
