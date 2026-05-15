package org.monostudio.jpa.repositories;

import java.util.List;
import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.StockTransferLine;

@org.springframework.stereotype.Repository
public interface StockTransferLinesRepository
    extends Repository<StockTransferLine> {

    List<StockTransferLine> findByStockTransferId(Long stockTransferId);
}
