package org.monostudio.jpa.services.predicates;

import org.monostudio.jpa.entities.BillingType;
import org.monostudio.jpa.entities.QBillingType;
import org.monostudio.jpa.services.PredicateService;

public interface BillingTypesPredicateService
    extends PredicateService<BillingType> {
    QBillingType basePath = QBillingType.billingType;
}
