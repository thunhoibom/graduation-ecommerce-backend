package org.monostudio.jpa.services.crud;

import org.monostudio.api.models.PersonPojo;
import org.monostudio.jpa.entities.Customer;
import org.monostudio.jpa.services.CrudService;

public interface CustomersCrudService
    extends CrudService<PersonPojo, Customer> {
}
