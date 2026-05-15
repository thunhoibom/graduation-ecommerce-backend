package org.monostudio.jpa.repositories;

import java.util.List;
import java.util.Optional;
import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.GoodsReceipt;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@org.springframework.stereotype.Repository
public interface GoodsReceiptsRepository
    extends Repository<GoodsReceipt> {

    Optional<GoodsReceipt> findByCode(String code);

    @Query("SELECT gr FROM GoodsReceipt gr WHERE gr.purchaseOrder.id = :purchaseOrderId ORDER BY gr.createdAt DESC")
    List<GoodsReceipt> findByPurchaseOrderId(@Param("purchaseOrderId") Long purchaseOrderId);
}
