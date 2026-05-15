package org.monostudio.jpa.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.monostudio.jpa.Repository;
import org.monostudio.jpa.entities.OrderDetail;

import java.util.List;

@org.springframework.stereotype.Repository
public interface OrderDetailsRepository
    extends Repository<OrderDetail> {

    @Query(value = "SELECT d FROM OrderDetail d WHERE d.order.id = :orderId")
    List<OrderDetail> findBySellId(@Param("orderId") Long orderId);

    /**
     * Units sold for a product in paid orders that are in progress or completed (excludes cancelled/abandoned).
     * Used to rank catalog search results.
     */
    @Query(value = """
        SELECT COALESCE(SUM(od.order_detail_units), 0)
        FROM order_details od
        INNER JOIN orders o ON od.order_id = o.order_id
        WHERE od.product_id = :productId
          AND o.payment_status = 'PAID'
          AND o.fulfillment_status IN ('PROCESSING', 'CONFIRMED', 'DELIVERY_COMPLETE', 'DELIVERED')
        """, nativeQuery = true)
    long sumPaidFulfilledUnitsForProduct(@Param("productId") long productId);
}
