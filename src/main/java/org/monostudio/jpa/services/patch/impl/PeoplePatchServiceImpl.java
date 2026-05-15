package org.monostudio.jpa.services.patch.impl;

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.PersonPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Person;
import org.monostudio.jpa.services.patch.PeoplePatchService;

import java.util.Map;

@Service
@NoArgsConstructor
public class PeoplePatchServiceImpl
    implements PeoplePatchService {

    @Override
    public Person patchExistingEntity(Map<String, Object> changes, Person existing) throws BadInputException {
        Person target = new Person(existing);

        if (changes.containsKey("idNumber")) {
            String idNumber = (String) changes.get("idNumber");
            if (!StringUtils.isBlank(idNumber)) {
                target.setIdNumber(idNumber);
            }
        }

        if (changes.containsKey("firstName")) {
            String firstName = (String) changes.get("firstName");
            if (!StringUtils.isBlank(firstName)) {
                target.setFirstName(firstName);
            }
        }

        if (changes.containsKey("lastName")) {
            String lastName = (String) changes.get("lastName");
            if (!StringUtils.isBlank(lastName)) {
                target.setLastName(lastName);
            }
        }

        if (changes.containsKey("email")) {
            String email = (String) changes.get("email");
            if (!StringUtils.isBlank(email)) {
                target.setEmail(email);
            }
        }

        if (changes.containsKey("phone1")) {
            String phone1 = (String) changes.get("phone1");
            target.setPhone1(phone1);
        }

        if (changes.containsKey("phone2")) {
            String phone2 = (String) changes.get("phone2");
            target.setPhone2(phone2);
        }

        return target;
    }

    @Override
    public Person patchExistingEntity(PersonPojo changes, Person existing) throws BadInputException {
        Person target = new Person(existing);

        if (changes.getFirstName() != null && !StringUtils.isBlank(changes.getFirstName())) {
            target.setFirstName(changes.getFirstName());
        }
        if (changes.getLastName() != null && !StringUtils.isBlank(changes.getLastName())) {
            target.setLastName(changes.getLastName());
        }
        if (changes.getIdNumber() != null && !StringUtils.isBlank(changes.getIdNumber())) {
            target.setIdNumber(changes.getIdNumber());
        }
        if (changes.getEmail() != null && !StringUtils.isBlank(changes.getEmail())) {
            target.setEmail(changes.getEmail());
        }
        if (changes.getPhone1() != null) {
            target.setPhone1(changes.getPhone1());
        }
        if (changes.getPhone2() != null) {
            target.setPhone2(changes.getPhone2());
        }

        return target;
    }
}
