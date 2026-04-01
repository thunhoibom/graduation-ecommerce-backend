package org.monostudio.jpa.services.crud.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.BillingTypePojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.BillingType;
import org.monostudio.jpa.repositories.BillingTypesRepository;
import org.monostudio.jpa.services.conversion.BillingTypesConverterService;
import org.monostudio.jpa.services.crud.BillingTypesCrudService;
import org.monostudio.jpa.services.crud.CrudGenericService;
import org.monostudio.jpa.services.patch.BillingTypesPatchService;

import java.util.Optional;

@Transactional
@Service
public class BillingTypesCrudServiceImpl
    extends CrudGenericService<BillingTypePojo, BillingType>
    implements BillingTypesCrudService {
    private final BillingTypesRepository typesRepository;

    @Autowired
    public BillingTypesCrudServiceImpl(
        BillingTypesRepository typesRepository,
        BillingTypesConverterService typesConverterService,
        BillingTypesPatchService typesPatchService
    ) {
        super(typesRepository, typesConverterService, typesPatchService);
        this.typesRepository = typesRepository;
    }

    @Override
    public Optional<BillingType> getExisting(BillingTypePojo input) throws BadInputException {
        String name = input.getName();
        if (StringUtils.isBlank(name)) {
            throw new BadInputException("Billing type has no name");
        } else {
            return typesRepository.findByName(name);
        }
    }
}
