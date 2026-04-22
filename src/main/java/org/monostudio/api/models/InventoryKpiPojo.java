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
public class InventoryKpiPojo {
    private long openPurchaseOrders;
    private long pendingTransferApprovals;
    private long pendingStockCountApprovals;
    private long approvedStockCountsToPost;
}
