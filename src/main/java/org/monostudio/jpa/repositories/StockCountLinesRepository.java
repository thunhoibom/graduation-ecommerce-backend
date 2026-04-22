package org.monostudio.jpa.repositories;

import java.util.List;
import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.StockCountLine;

@org.springframework.stereotype.Repository
public interface StockCountLinesRepository
    extends Repository<StockCountLine> {

    List<StockCountLine> findByStockCountSessionId(Long stockCountSessionId);
}
