package org.monostudio.jpa.services.predicates;

import org.monostudio.jpa.entities.QBillingType;
import org.monostudio.jpa.services.PredicateService;

public interface BillingTypesPredicateService
    extends PredicateService {
    QBillingType basePath = QBillingType.billingType;
}
