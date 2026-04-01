package org.monostudio.jpa.services.patch;

import org.monostudio.api.models.PersonPojo;
import org.monostudio.jpa.entities.Customer;
import org.monostudio.jpa.services.PatchService;

public interface CustomersPatchService
    extends PatchService<PersonPojo, Customer> {
}
