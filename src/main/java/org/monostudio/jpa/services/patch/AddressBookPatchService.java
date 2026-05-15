package org.monostudio.jpa.services.patch;

import org.monostudio.api.models.AddressBookPojo;
import org.monostudio.jpa.entities.AddressBook;
import org.monostudio.jpa.services.PatchService;

public interface AddressBookPatchService
    extends PatchService<AddressBookPojo, AddressBook> {
}
