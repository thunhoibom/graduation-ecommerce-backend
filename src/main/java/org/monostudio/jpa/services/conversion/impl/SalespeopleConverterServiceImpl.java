package org.monostudio.jpa.services.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.PersonPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Person;
import org.monostudio.jpa.entities.Salesperson;
import org.monostudio.jpa.services.conversion.PeopleConverterService;
import org.monostudio.jpa.services.conversion.SalespeopleConverterService;

@Service
public class SalespeopleConverterServiceImpl
    implements SalespeopleConverterService {
    private final PeopleConverterService peopleConverterService;

    @Autowired
    public SalespeopleConverterServiceImpl(
        PeopleConverterService peopleConverterService
    ) {
        this.peopleConverterService = peopleConverterService;
    }

    @Override
    public PersonPojo convertToPojo(Salesperson source) {
        return peopleConverterService.convertToPojo(source.getPerson());
    }

    @Override
    public Salesperson convertToNewEntity(PersonPojo source) throws BadInputException {
        Person targetPerson = peopleConverterService.convertToNewEntity(source);
        return Salesperson.builder()
            .person(targetPerson)
            .build();
    }

    @Override
    public Salesperson applyChangesToExistingEntity(PersonPojo source, Salesperson target) {
        throw new UnsupportedOperationException("This method is deprecated");
    }
}
