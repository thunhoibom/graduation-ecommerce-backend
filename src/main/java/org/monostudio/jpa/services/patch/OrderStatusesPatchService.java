package org.monostudio.jpa.services.patch;

import org.monostudio.api.models.OrderStatusPojo;
import org.monostudio.jpa.entities.OrderStatus;
import org.monostudio.jpa.services.PatchService;

public interface OrderStatusesPatchService
    extends PatchService<OrderStatusPojo, OrderStatus> {
}
