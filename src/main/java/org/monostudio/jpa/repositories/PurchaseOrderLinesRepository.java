package org.monostudio.jpa.repositories;

import java.util.List;
import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.PurchaseOrderLine;

@org.springframework.stereotype.Repository
public interface PurchaseOrderLinesRepository
    extends Repository<PurchaseOrderLine> {

    List<PurchaseOrderLine> findByPurchaseOrderId(Long purchaseOrderId);
}
