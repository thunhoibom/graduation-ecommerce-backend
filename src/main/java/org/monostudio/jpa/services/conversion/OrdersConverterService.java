package org.monostudio.jpa.services.conversion;

import org.monostudio.api.models.OrderDetailPojo;
import org.monostudio.api.models.OrderPojo;
import org.monostudio.jpa.entities.Order;
import org.monostudio.jpa.entities.OrderDetail;
import org.monostudio.jpa.services.ConverterService;

public interface OrdersConverterService
    extends ConverterService<OrderPojo, Order> {
    OrderDetailPojo convertDetailToPojo(OrderDetail source);

    OrderDetail convertDetailToNewEntity(OrderDetailPojo detail);
}
