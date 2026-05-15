package org.monostudio.jpa.services.patch.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.PersonPojo;
import org.monostudio.common.Utils;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Person;
import org.monostudio.jpa.entities.Salesperson;
import org.monostudio.jpa.services.patch.PeoplePatchService;
import org.monostudio.jpa.services.patch.SalespeoplePatchService;

import java.util.Map;

@Service
public class SalespeoplePatchServiceImpl
    implements SalespeoplePatchService {
    private final PeoplePatchService peoplePatchService;

    @Autowired
    public SalespeoplePatchServiceImpl(
        PeoplePatchService peoplePatchService
    ) {
        this.peoplePatchService = peoplePatchService;
    }

    @Override
    public Salesperson patchExistingEntity(Map<String, Object> changes, Salesperson existing) throws BadInputException {
        Salesperson target = new Salesperson(existing);

        Map<String, Object> personChanges = Utils.copyMapWithUnprefixedEntries(changes, "person.");
        if (!personChanges.isEmpty()) {
            Person existingPerson = existing.getPerson();
            Person person = peoplePatchService.patchExistingEntity(personChanges, existingPerson);
            target.setPerson(person);
        }

        return target;
    }

    @Override
    public Salesperson patchExistingEntity(PersonPojo changes, Salesperson existing) throws BadInputException {
        throw new UnsupportedOperationException("This method has been deprecated");
    }
}
