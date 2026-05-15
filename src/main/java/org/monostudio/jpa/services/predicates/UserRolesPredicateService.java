package org.monostudio.jpa.services.predicates;

import org.monostudio.jpa.entities.UserRole;
import org.monostudio.jpa.entities.QUserRole;
import org.monostudio.jpa.services.PredicateService;

public interface UserRolesPredicateService
    extends PredicateService<UserRole> {
    QUserRole basePath = QUserRole.userRole;
}
