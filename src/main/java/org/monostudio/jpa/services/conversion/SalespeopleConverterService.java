package org.monostudio.jpa.services.conversion;

import org.monostudio.api.models.PersonPojo;
import org.monostudio.jpa.entities.Salesperson;
import org.monostudio.jpa.services.ConverterService;

public interface SalespeopleConverterService
    extends ConverterService<PersonPojo, Salesperson> {
}
