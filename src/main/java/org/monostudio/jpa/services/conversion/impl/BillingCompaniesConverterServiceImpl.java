package org.monostudio.jpa.services.conversion.impl;

import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.BillingCompanyPojo;
import org.monostudio.jpa.entities.BillingCompany;
import org.monostudio.jpa.services.conversion.BillingCompaniesConverterService;

@Service
@NoArgsConstructor
public class BillingCompaniesConverterServiceImpl
    implements BillingCompaniesConverterService {

    @Override
    public BillingCompanyPojo convertToPojo(BillingCompany source) {
        return BillingCompanyPojo.builder()
            .idNumber(source.getIdNumber())
            .name(source.getName())
            .build();
    }

    @Override
    public BillingCompany convertToNewEntity(BillingCompanyPojo source) {
        return BillingCompany.builder()
            .idNumber(source.getIdNumber())
            .name(source.getName())
            .build();
    }

    @Override
    public BillingCompany applyChangesToExistingEntity(BillingCompanyPojo source, BillingCompany target) {
        throw new UnsupportedOperationException("This method is deprecated");
    }
}
