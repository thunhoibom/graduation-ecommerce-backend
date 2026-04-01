package org.monostudio.jpa.services.predicates;

import org.monostudio.jpa.entities.QOrderStatus;
import org.monostudio.jpa.services.PredicateService;

public interface OrderStatusesPredicateService
    extends PredicateService {
    QOrderStatus basePath = QOrderStatus.orderStatus;
}
