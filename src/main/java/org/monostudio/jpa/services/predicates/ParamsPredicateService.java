package org.monostudio.jpa.services.predicates;

import org.monostudio.jpa.entities.Param;
import org.monostudio.jpa.entities.QParam;
import org.monostudio.jpa.services.PredicateService;

public interface ParamsPredicateService
    extends PredicateService<Param> {
    QParam basePath = QParam.param;
}