package org.monostudio.jpa.services.patch;

import org.monostudio.api.models.PersonPojo;
import org.monostudio.jpa.entities.Person;
import org.monostudio.jpa.services.PatchService;

public interface PeoplePatchService
    extends PatchService<PersonPojo, Person> {
}
