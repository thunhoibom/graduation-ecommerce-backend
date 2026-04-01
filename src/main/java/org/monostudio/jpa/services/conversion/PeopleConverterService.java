package org.monostudio.jpa.services.conversion;

import org.monostudio.api.models.PersonPojo;
import org.monostudio.jpa.entities.Person;
import org.monostudio.jpa.services.ConverterService;

public interface PeopleConverterService
    extends ConverterService<PersonPojo, Person> {
}
