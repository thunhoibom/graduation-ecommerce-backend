package org.monostudio.jpa.services.conversion;

import org.monostudio.api.models.AddressBookPojo;
import org.monostudio.jpa.entities.AddressBook;
import org.monostudio.jpa.services.ConverterService;

public interface AddressBookConverterService
    extends ConverterService<AddressBookPojo, AddressBook> {
}
