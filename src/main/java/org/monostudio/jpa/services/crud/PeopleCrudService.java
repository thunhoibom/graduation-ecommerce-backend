package org.monostudio.jpa.services.crud;

import org.monostudio.api.models.PersonPojo;
import org.monostudio.jpa.entities.Person;
import org.monostudio.jpa.services.CrudService;

public interface PeopleCrudService
    extends CrudService<PersonPojo, Person> {
}
