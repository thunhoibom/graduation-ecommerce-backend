package org.monostudio.jpa.services.crud.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.PersonPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Person;
import org.monostudio.jpa.repositories.PeopleRepository;
import org.monostudio.jpa.services.conversion.PeopleConverterService;
import org.monostudio.jpa.services.crud.CrudGenericService;
import org.monostudio.jpa.services.crud.PeopleCrudService;
import org.monostudio.jpa.services.patch.PeoplePatchService;

import java.util.Optional;

@Transactional
@Service
public class PeopleCrudServiceImpl
    extends CrudGenericService<PersonPojo, Person>
    implements PeopleCrudService {
    private final PeopleRepository peopleRepository;

    @Autowired
    public PeopleCrudServiceImpl(
        PeopleRepository peopleRepository,
        PeopleConverterService peopleConverterService,
        PeoplePatchService peoplePatchService
    ) {
        super(peopleRepository, peopleConverterService, peoplePatchService);
        this.peopleRepository = peopleRepository;
    }

    @Override
    public Optional<Person> getExisting(PersonPojo input) throws BadInputException {
        String idCard = input.getIdNumber();
        if (StringUtils.isBlank(idCard)) {
            throw new BadInputException("Customer does not have ID card");
        } else {
            return peopleRepository.findByIdNumber(idCard);
        }
    }
}
