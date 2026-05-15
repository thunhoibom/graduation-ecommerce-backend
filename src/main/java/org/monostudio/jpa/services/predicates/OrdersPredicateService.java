package org.monostudio.jpa.services.predicates;

import org.monostudio.jpa.entities.Order;
import org.monostudio.jpa.entities.QOrder;
import org.monostudio.jpa.services.PredicateService;

public interface OrdersPredicateService
    extends PredicateService<Order> {
    QOrder basePath = QOrder.order;
}
