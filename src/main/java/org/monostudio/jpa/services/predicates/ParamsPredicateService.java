package org.monostudio.jpa.services.predicates;

import org.monostudio.jpa.entities.QParam;
import org.monostudio.jpa.services.PredicateService;

public interface ParamsPredicateService
    extends PredicateService {
    QParam basePath = QParam.param;
}