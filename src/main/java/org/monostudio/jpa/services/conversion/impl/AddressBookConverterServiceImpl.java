package org.monostudio.jpa.services.conversion.impl;

import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.AddressBookPojo;
import org.monostudio.api.models.AddressPojo;
import org.monostudio.jpa.entities.AddressBook;
import org.monostudio.jpa.services.conversion.AddressBookConverterService;
import org.monostudio.jpa.services.conversion.AddressesConverterService;

@Service
@NoArgsConstructor
public class AddressBookConverterServiceImpl
    implements AddressBookConverterService {

    private AddressesConverterService addressesConverterService;

    @Autowired
    public AddressBookConverterServiceImpl(AddressesConverterService addressesConverterService) {
        this.addressesConverterService = addressesConverterService;
    }

    @Override
    public AddressBookPojo convertToPojo(AddressBook source) {
        AddressPojo addressPojo = addressesConverterService.convertToPojo(source.getAddress());
        return AddressBookPojo.builder()
            .label(source.getLabel())
            .defaultShipping(source.isDefaultShipping())
            .defaultBilling(source.isDefaultBilling())
            .address(addressPojo)
            .createdAt(source.getCreatedAt())
            .updatedAt(source.getUpdatedAt())
            .build();
    }

    @Override
    public AddressBook convertToNewEntity(AddressBookPojo source) {
        AddressBook target = AddressBook.builder()
            .label(source.getLabel())
            .defaultShipping(source.getDefaultShipping() != null ? source.getDefaultShipping() : false)
            .defaultBilling(source.getDefaultBilling() != null ? source.getDefaultBilling() : false)
            .build();
        return target;
    }

    @Override
    public AddressBook applyChangesToExistingEntity(AddressBookPojo source, AddressBook target) {
        throw new UnsupportedOperationException("This method is deprecated");
    }
}
