package org.monostudio.jpa.services.conversion;

import org.monostudio.api.models.PersonPojo;
import org.monostudio.jpa.entities.Customer;
import org.monostudio.jpa.services.ConverterService;

public interface CustomersConverterService
    extends ConverterService<PersonPojo, Customer> {
}
