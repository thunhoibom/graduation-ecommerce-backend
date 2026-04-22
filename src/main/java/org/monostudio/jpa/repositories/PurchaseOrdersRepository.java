package org.monostudio.jpa.repositories;

import java.util.List;
import java.util.Optional;
import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.PurchaseOrder;
import org.monostudio.jpa.entities.PurchaseOrder.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@org.springframework.stereotype.Repository
public interface PurchaseOrdersRepository
    extends Repository<PurchaseOrder> {

    Optional<PurchaseOrder> findByCode(String code);

    @Query("SELECT po FROM PurchaseOrder po JOIN FETCH po.supplier WHERE po.id = :id")
    Optional<PurchaseOrder> findByIdDeep(@Param("id") Long id);

    @Query("""
        SELECT DISTINCT po FROM PurchaseOrder po
        JOIN FETCH po.supplier s
        WHERE (:status IS NULL OR po.status = :status)
        ORDER BY po.createdAt DESC
        """)
    List<PurchaseOrder> findAllByStatusDeep(@Param("status") PurchaseOrderStatus status);

    long countByStatusIn(List<PurchaseOrderStatus> statuses);
}
