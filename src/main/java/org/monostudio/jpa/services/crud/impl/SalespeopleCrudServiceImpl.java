package org.monostudio.jpa.services.crud.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.PersonPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Salesperson;
import org.monostudio.jpa.repositories.SalespeopleRepository;
import org.monostudio.jpa.services.conversion.SalespeopleConverterService;
import org.monostudio.jpa.services.crud.CrudGenericService;
import org.monostudio.jpa.services.crud.SalespeopleCrudService;
import org.monostudio.jpa.services.patch.SalespeoplePatchService;

import java.util.Optional;

@Transactional
@Service
public class SalespeopleCrudServiceImpl
    extends CrudGenericService<PersonPojo, Salesperson>
    implements SalespeopleCrudService {
    private final SalespeopleRepository salespeopleRepository;

    @Autowired
    public SalespeopleCrudServiceImpl(
        SalespeopleRepository salespeopleRepository,
        SalespeopleConverterService salespeopleConverterService,
        SalespeoplePatchService salespeoplePatchService
    ) {
        super(salespeopleRepository, salespeopleConverterService, salespeoplePatchService);
        this.salespeopleRepository = salespeopleRepository;
    }

    @Override
    public Optional<Salesperson> getExisting(PersonPojo input) throws BadInputException {
        String idNumber = input.getIdNumber();
        if (StringUtils.isBlank(idNumber)) {
            throw new BadInputException("Salesperson does not have an ID card");
        } else {
            return salespeopleRepository.findByPersonIdNumber(idNumber);
        }
    }
}
