package org.monostudio.jpa.repositories;

import java.util.Collection;
import java.util.List;
import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.PurchaseOrderLine;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@org.springframework.stereotype.Repository
public interface PurchaseOrderLinesRepository
    extends Repository<PurchaseOrderLine> {

    List<PurchaseOrderLine> findByPurchaseOrderId(Long purchaseOrderId);

    @Query("""
        SELECT l.purchaseOrder.id AS purchaseOrderId,
               COUNT(l) AS lineCount,
               COALESCE(SUM(l.orderedQty * COALESCE(l.unitCost, 0)), 0) AS orderedTotalAmount,
               COALESCE(SUM(l.receivedQty * COALESCE(l.unitCost, 0)), 0) AS receivedTotalAmount
        FROM PurchaseOrderLine l
        WHERE l.purchaseOrder.id IN :purchaseOrderIds
        GROUP BY l.purchaseOrder.id
        """)
    List<PurchaseOrderLineTotalsProjection> summarizeByPurchaseOrderIds(
        @Param("purchaseOrderIds") Collection<Long> purchaseOrderIds
    );
}
