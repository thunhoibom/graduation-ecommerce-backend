package org.monostudio.jpa.services.conversion;

import org.monostudio.api.models.OrderStatusPojo;
import org.monostudio.jpa.entities.OrderStatus;
import org.monostudio.jpa.services.ConverterService;

public interface OrderStatusesConverterService
    extends ConverterService<OrderStatusPojo, OrderStatus> {
}
