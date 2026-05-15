package org.monostudio.jpa.services.predicates;

import org.monostudio.jpa.entities.User;
import org.monostudio.jpa.entities.QUser;
import org.monostudio.jpa.services.PredicateService;

public interface UsersPredicateService
    extends PredicateService<User> {
    QUser basePath = QUser.user;
}
