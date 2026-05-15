package org.monostudio.api.services;

import java.util.List;
import org.monostudio.api.models.StockCountSessionPojo;
import org.monostudio.api.models.inventory.StockCountLineUpsertRequest;
import org.monostudio.api.models.inventory.StockCountSessionCreateRequest;

public interface StockCountService {
    List<StockCountSessionPojo> list(String status);
    StockCountSessionPojo getById(Long id);
    StockCountSessionPojo create(StockCountSessionCreateRequest request);
    StockCountSessionPojo start(Long id, Long actorId, String note);
    StockCountSessionPojo updateCountLines(Long id, StockCountLineUpsertRequest request);
    StockCountSessionPojo completeCount(Long id, Long actorId, String note);
    StockCountSessionPojo approve(Long id, Long actorId, String note);
    StockCountSessionPojo postVariance(Long id, Long actorId, String note);
}
