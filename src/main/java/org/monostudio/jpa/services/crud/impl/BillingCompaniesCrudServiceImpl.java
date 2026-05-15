package org.monostudio.jpa.services.crud.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.BillingCompanyPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.BillingCompany;
import org.monostudio.jpa.repositories.BillingCompaniesRepository;
import org.monostudio.jpa.services.conversion.BillingCompaniesConverterService;
import org.monostudio.jpa.services.crud.BillingCompaniesCrudService;
import org.monostudio.jpa.services.crud.CrudGenericService;
import org.monostudio.jpa.services.patch.BillingCompaniesPatchService;

import java.util.Optional;

@Transactional
@Service
public class BillingCompaniesCrudServiceImpl
    extends CrudGenericService<BillingCompanyPojo, BillingCompany>
    implements BillingCompaniesCrudService {
    private final BillingCompaniesRepository companiesRepository;

    @Autowired
    public BillingCompaniesCrudServiceImpl(
        BillingCompaniesRepository companiesRepository,
        BillingCompaniesConverterService companiesConverterService,
        BillingCompaniesPatchService companiesPatchService
    ) {
        super(companiesRepository, companiesConverterService, companiesPatchService);
        this.companiesRepository = companiesRepository;
    }

    @Override
    public Optional<BillingCompany> getExisting(BillingCompanyPojo input) throws BadInputException {
        String idNumber = input.getIdNumber();
        if (StringUtils.isBlank(idNumber)) {
            throw new BadInputException("Billing company has no id number");
        } else {
            return companiesRepository.findByIdNumber(idNumber);
        }
    }
}
