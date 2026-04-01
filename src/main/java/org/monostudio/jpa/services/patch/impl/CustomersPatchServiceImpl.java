package org.monostudio.jpa.services.patch.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.PersonPojo;
import org.monostudio.common.Utils;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Customer;
import org.monostudio.jpa.entities.Person;
import org.monostudio.jpa.services.patch.CustomersPatchService;
import org.monostudio.jpa.services.patch.PeoplePatchService;

import java.util.Map;

import static org.monostudio.config.Constants.PERSON_DATA_MAP_KEYS_PREFIX;

@Service
public class CustomersPatchServiceImpl
    implements CustomersPatchService {
    private final PeoplePatchService peoplePatchService;

    @Autowired
    public CustomersPatchServiceImpl(
        PeoplePatchService peoplePatchService
    ) {
        this.peoplePatchService = peoplePatchService;
    }

    @Override
    public Customer patchExistingEntity(Map<String, Object> changes, Customer existing) throws BadInputException {
        Customer target = new Customer(existing);

        Map<String, Object> personChanges = Utils.copyMapWithUnprefixedEntries(changes, PERSON_DATA_MAP_KEYS_PREFIX);
        if (!personChanges.isEmpty()) {
            Person existingPerson = existing.getPerson();
            Person person = peoplePatchService.patchExistingEntity(personChanges, existingPerson);
            target.setPerson(person);
        }

        return target;
    }

    @Override
    public Customer patchExistingEntity(PersonPojo changes, Customer existing) throws BadInputException {
        throw new UnsupportedOperationException("This method has been deprecated");
    }
}
