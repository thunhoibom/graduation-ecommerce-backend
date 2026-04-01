package org.monostudio.jpa.services.predicates;

import org.monostudio.jpa.entities.QBillingCompany;
import org.monostudio.jpa.services.PredicateService;

public interface BillingCompaniesPredicateService
    extends PredicateService {
    QBillingCompany basePath = QBillingCompany.billingCompany;
}
