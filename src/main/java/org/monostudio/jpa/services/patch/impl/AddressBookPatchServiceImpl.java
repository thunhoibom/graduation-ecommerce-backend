package org.monostudio.jpa.services.patch.impl;

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.AddressBookPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.AddressBook;
import org.monostudio.jpa.services.patch.AddressBookPatchService;

import java.util.Map;

@Service
@NoArgsConstructor
public class AddressBookPatchServiceImpl
    implements AddressBookPatchService {

    @Override
    public AddressBook patchExistingEntity(Map<String, Object> changes, AddressBook existing) throws BadInputException {
        AddressBook target = new AddressBook(existing);

        if (changes.containsKey("label")) {
            String label = (String) changes.get("label");
            if (!StringUtils.isBlank(label)) {
                target.setLabel(label);
            }
        }

        if (changes.containsKey("defaultShipping")) {
            Boolean defaultShipping = (Boolean) changes.get("defaultShipping");
            if (defaultShipping != null) {
                target.setDefaultShipping(defaultShipping);
            }
        }

        if (changes.containsKey("defaultBilling")) {
            Boolean defaultBilling = (Boolean) changes.get("defaultBilling");
            if (defaultBilling != null) {
                target.setDefaultBilling(defaultBilling);
            }
        }

        return target;
    }

    @Override
    public AddressBook patchExistingEntity(AddressBookPojo changes, AddressBook existing) throws BadInputException {
        throw new UnsupportedOperationException("This method is deprecated");
    }
}
