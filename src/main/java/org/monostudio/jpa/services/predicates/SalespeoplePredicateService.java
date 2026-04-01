package org.monostudio.jpa.services.predicates;

import org.monostudio.jpa.entities.QSalesperson;
import org.monostudio.jpa.services.PredicateService;

public interface SalespeoplePredicateService
    extends PredicateService {
    QSalesperson basePath = QSalesperson.salesperson;
}
