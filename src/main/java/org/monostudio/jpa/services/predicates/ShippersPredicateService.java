package org.monostudio.jpa.services.predicates;

import org.monostudio.jpa.entities.QShipper;
import org.monostudio.jpa.services.PredicateService;

public interface ShippersPredicateService
    extends PredicateService {
    QShipper basePath = QShipper.shipper;
}
