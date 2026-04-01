package org.monostudio.jpa.services.conversion.impl;

import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.BillingTypePojo;
import org.monostudio.jpa.entities.BillingType;
import org.monostudio.jpa.services.conversion.BillingTypesConverterService;

@Service
@NoArgsConstructor
public class BillingTypesConverterServiceImpl
    implements BillingTypesConverterService {

    @Override
    public BillingTypePojo convertToPojo(BillingType source) {
        return BillingTypePojo.builder()
            .name(source.getName())
            .build();
    }

    @Override
    public BillingType convertToNewEntity(BillingTypePojo source) {
        return BillingType.builder()
            .name(source.getName())
            .build();
    }

    @Override
    public BillingType applyChangesToExistingEntity(BillingTypePojo source, BillingType target) {
        throw new UnsupportedOperationException("This method is deprecated");
    }
}
