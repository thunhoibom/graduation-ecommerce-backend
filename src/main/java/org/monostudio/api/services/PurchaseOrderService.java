package org.monostudio.api.services;

import java.util.List;
import org.monostudio.api.models.GoodsReceiptPojo;
import org.monostudio.api.models.PurchaseOrderPojo;
import org.monostudio.api.models.inventory.GoodsReceiptCreateRequest;
import org.monostudio.api.models.inventory.PurchaseOrderCreateRequest;

public interface PurchaseOrderService {
    List<PurchaseOrderPojo> list(String status);
    PurchaseOrderPojo getById(Long id);
    PurchaseOrderPojo create(PurchaseOrderCreateRequest request);
    PurchaseOrderPojo submit(Long id, Long actorId, String note);
    PurchaseOrderPojo approve(Long id, Long actorId, String note);
    PurchaseOrderPojo cancel(Long id, Long actorId, String note);
    GoodsReceiptPojo receive(Long id, GoodsReceiptCreateRequest request);
}
