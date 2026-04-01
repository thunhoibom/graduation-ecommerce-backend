package org.monostudio.jpa.services.conversion.impl;

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.AddressPojo;
import org.monostudio.jpa.entities.Address;
import org.monostudio.jpa.services.conversion.AddressesConverterService;

@Service
@NoArgsConstructor
public class AddressesConverterServiceImpl
    implements AddressesConverterService {

    @Override
    public AddressPojo convertToPojo(Address source) {
        AddressPojo target = AddressPojo.builder()
            .city(source.getCity())
            .municipality(source.getMunicipality())
            .firstLine(source.getFirstLine())
            .build();
        if (!StringUtils.isBlank(source.getSecondLine())) {
            target.setSecondLine(source.getSecondLine());
        }
        if (!StringUtils.isBlank(source.getPostalCode())) {
            target.setPostalCode(source.getPostalCode());
        }
        if (!StringUtils.isBlank(source.getNotes())) {
            target.setNotes(source.getNotes());
        }
        return target;
    }

    @Override
    public Address convertToNewEntity(AddressPojo source) {
        return Address.builder()
            .firstLine(source.getFirstLine())
            .secondLine(source.getSecondLine())
            .city(source.getCity())
            .municipality(source.getMunicipality())
            .postalCode(source.getPostalCode())
            .notes((source.getNotes()))
            .build();
    }

    @Override
    public Address applyChangesToExistingEntity(AddressPojo source, Address target) {
        throw new UnsupportedOperationException("This method is deprecated");
    }
}
