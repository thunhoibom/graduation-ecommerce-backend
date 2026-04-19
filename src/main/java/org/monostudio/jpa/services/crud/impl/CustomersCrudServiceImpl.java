package org.monostudio.jpa.services.crud.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.PersonPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Customer;
import org.monostudio.jpa.repositories.CustomersRepository;
import org.monostudio.jpa.services.conversion.CustomersConverterService;
import org.monostudio.jpa.services.crud.CrudGenericService;
import org.monostudio.jpa.services.crud.CustomersCrudService;
import org.monostudio.jpa.services.patch.CustomersPatchService;

import java.util.Optional;

@Transactional
@Service
public class CustomersCrudServiceImpl
    extends CrudGenericService<PersonPojo, Customer>
    implements CustomersCrudService {
    private final CustomersRepository customersRepository;

    @Autowired
    public CustomersCrudServiceImpl(
        CustomersRepository customersRepository,
        CustomersConverterService customersConverterService,
        CustomersPatchService customersPatchService
    ) {
        super(customersRepository, customersConverterService, customersPatchService);
        this.customersRepository = customersRepository;
    }

    @Override
    public Optional<Customer> getExisting(PersonPojo input) throws BadInputException {
        String idNumber = input.getIdNumber();
        if (StringUtils.isBlank(idNumber)) {
            // Fallback to email for guests/missing ID
            if (StringUtils.isNotBlank(input.getEmail())) {
                return customersRepository.findAllByPersonEmail(input.getEmail()).stream().findFirst();
            }
            return Optional.empty();
        } else {
            return customersRepository.findByPersonIdNumber(idNumber);
        }
    }
}
