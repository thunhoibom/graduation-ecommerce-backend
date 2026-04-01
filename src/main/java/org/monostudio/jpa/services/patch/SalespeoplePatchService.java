package org.monostudio.jpa.services.patch;

import org.monostudio.api.models.PersonPojo;
import org.monostudio.jpa.entities.Salesperson;
import org.monostudio.jpa.services.PatchService;

public interface SalespeoplePatchService
    extends PatchService<PersonPojo, Salesperson> {
}
