package org.monostudio.jpa.services.predicates;

import org.monostudio.jpa.entities.Customer;
import org.monostudio.jpa.entities.QCustomer;
import org.monostudio.jpa.services.PredicateService;

public interface CustomersPredicateService
    extends PredicateService<Customer> {
    QCustomer basePath = QCustomer.customer;
}
