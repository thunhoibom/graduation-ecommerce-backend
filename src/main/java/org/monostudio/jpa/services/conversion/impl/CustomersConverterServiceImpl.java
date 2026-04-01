package org.monostudio.jpa.services.conversion.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.PersonPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Customer;
import org.monostudio.jpa.entities.Person;
import org.monostudio.jpa.services.conversion.CustomersConverterService;
import org.monostudio.jpa.services.conversion.PeopleConverterService;

@Service
public class CustomersConverterServiceImpl
    implements CustomersConverterService {
    private final PeopleConverterService peopleConverterService;

    @Autowired
    public CustomersConverterServiceImpl(
        PeopleConverterService peopleConverterService
    ) {
        this.peopleConverterService = peopleConverterService;
    }

    @Override
    public PersonPojo convertToPojo(Customer source) {
        return peopleConverterService.convertToPojo(source.getPerson());
    }

    @Override
    public Customer convertToNewEntity(PersonPojo source) throws BadInputException {
        Person targetPerson = peopleConverterService.convertToNewEntity(source);
        return Customer.builder()
            .person(targetPerson)
            .build();
    }

    @Override
    public Customer applyChangesToExistingEntity(PersonPojo source, Customer target) {
        throw new UnsupportedOperationException("This method is deprecated");
    }
}
