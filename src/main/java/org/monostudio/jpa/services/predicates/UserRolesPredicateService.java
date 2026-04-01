package org.monostudio.jpa.services.predicates;

import org.monostudio.jpa.entities.QUserRole;
import org.monostudio.jpa.services.PredicateService;

public interface UserRolesPredicateService
    extends PredicateService {
    QUserRole basePath = QUserRole.userRole;
}
