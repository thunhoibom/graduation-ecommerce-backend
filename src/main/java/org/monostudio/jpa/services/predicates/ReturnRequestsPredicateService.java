package org.monostudio.jpa.services.predicates;

import org.monostudio.jpa.entities.QReturnRequest;
import org.monostudio.jpa.services.PredicateService;

public interface ReturnRequestsPredicateService
    extends PredicateService {
    QReturnRequest basePath = QReturnRequest.returnRequest;
}
