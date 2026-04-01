package org.monostudio.jpa.services.predicates;

import org.monostudio.jpa.entities.QCustomer;
import org.monostudio.jpa.services.PredicateService;

public interface CustomersPredicateService
    extends PredicateService {
    QCustomer basePath = QCustomer.customer;
}
