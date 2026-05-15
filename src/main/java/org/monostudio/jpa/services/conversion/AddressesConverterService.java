package org.monostudio.jpa.services.conversion;

import org.monostudio.api.models.AddressPojo;
import org.monostudio.jpa.entities.Address;
import org.monostudio.jpa.services.ConverterService;

public interface AddressesConverterService
    extends ConverterService<AddressPojo, Address> {
}
