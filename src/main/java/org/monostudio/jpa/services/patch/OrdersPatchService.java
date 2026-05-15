package org.monostudio.jpa.services.patch;

import org.monostudio.api.models.OrderPojo;
import org.monostudio.jpa.entities.Order;
import org.monostudio.jpa.services.PatchService;

public interface OrdersPatchService
    extends PatchService<OrderPojo, Order> {
}
