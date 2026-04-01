package org.monostudio.jpa.services.predicates;

import org.monostudio.jpa.entities.QPerson;
import org.monostudio.jpa.services.PredicateService;

public interface PeoplePredicateService
    extends PredicateService {
    QPerson basePath = QPerson.person;
}
