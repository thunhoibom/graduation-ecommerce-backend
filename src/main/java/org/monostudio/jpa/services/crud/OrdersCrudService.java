package org.monostudio.jpa.services.crud;

import org.monostudio.api.models.OrderPojo;
import org.monostudio.jpa.entities.Order;
import org.monostudio.jpa.services.CrudService;

public interface OrdersCrudService
    extends CrudService<OrderPojo, Order> {
}
