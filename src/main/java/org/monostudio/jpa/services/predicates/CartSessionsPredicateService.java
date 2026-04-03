package org.monostudio.jpa.services.predicates;

import org.monostudio.jpa.entities.QCartSession;
import org.monostudio.jpa.services.PredicateService;

public interface CartSessionsPredicateService
    extends PredicateService {
    QCartSession basePath = QCartSession.cartSession;
}
