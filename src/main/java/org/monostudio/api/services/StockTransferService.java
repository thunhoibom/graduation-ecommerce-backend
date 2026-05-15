package org.monostudio.api.services;

import java.util.List;
import org.monostudio.api.models.StockTransferPojo;
import org.monostudio.api.models.inventory.StockTransferCreateRequest;

public interface StockTransferService {
    List<StockTransferPojo> list(String status);
    StockTransferPojo getById(Long id);
    StockTransferPojo create(StockTransferCreateRequest request);
    StockTransferPojo submit(Long id, Long actorId, String note);
    StockTransferPojo approve(Long id, Long actorId, String note);
    StockTransferPojo complete(Long id, Long actorId, String note);
    StockTransferPojo cancel(Long id, Long actorId, String note);
}
