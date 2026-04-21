package org.monostudio.jpa.services.predicates;

import org.monostudio.jpa.entities.ShippingMethod;
import org.monostudio.jpa.entities.QShippingMethod;
import org.monostudio.jpa.services.PredicateService;

public interface ShippingMethodsPredicateService
    extends PredicateService<ShippingMethod> {
    QShippingMethod basePath = QShippingMethod.shippingMethod;
}
