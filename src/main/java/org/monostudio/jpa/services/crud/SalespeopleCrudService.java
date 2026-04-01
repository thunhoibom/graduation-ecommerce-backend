package org.monostudio.jpa.services.crud;

import org.monostudio.api.models.PersonPojo;
import org.monostudio.jpa.entities.Salesperson;
import org.monostudio.jpa.services.CrudService;

public interface SalespeopleCrudService
    extends CrudService<PersonPojo, Salesperson> {
}
