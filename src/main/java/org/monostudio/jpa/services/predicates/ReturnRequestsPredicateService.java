package org.monostudio.jpa.services.predicates;

import org.monostudio.jpa.entities.ReturnRequest;
import org.monostudio.jpa.entities.QReturnRequest;
import org.monostudio.jpa.services.PredicateService;

public interface ReturnRequestsPredicateService
    extends PredicateService<ReturnRequest> {
    QReturnRequest basePath = QReturnRequest.returnRequest;
}
