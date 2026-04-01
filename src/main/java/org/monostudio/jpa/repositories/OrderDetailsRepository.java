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
}
