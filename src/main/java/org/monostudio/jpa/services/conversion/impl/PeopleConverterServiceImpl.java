package org.monostudio.jpa.services.conversion.impl;

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.monostudio.common.EmailNormalizer;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.PersonPojo;
import org.monostudio.jpa.entities.Person;
import org.monostudio.jpa.services.conversion.PeopleConverterService;

@Service
@NoArgsConstructor
public class PeopleConverterServiceImpl
    implements PeopleConverterService {

    @Override
    public PersonPojo convertToPojo(Person source) {
        PersonPojo target = PersonPojo.builder()
            .idNumber(source.getIdNumber())
            .firstName(source.getFirstName())
            .lastName(source.getLastName())
            .email(source.getEmail())
            .build();
        if (source.getPhone1()!=null) {
            target.setPhone1(source.getPhone1());
        }
        if (source.getPhone2()!=null) {
            target.setPhone2(source.getPhone2());
        }
        return target;
    }

    @Override
    public Person convertToNewEntity(PersonPojo source) {
        String normalizedEmail = EmailNormalizer.normalize(source.getEmail());
        Person target = Person.builder()
            .firstName(source.getFirstName())
            .lastName(source.getLastName())
            .idNumber(StringUtils.isNotBlank(source.getIdNumber()) ? source.getIdNumber() : "GUEST-" + java.util.UUID.randomUUID().toString().substring(0, 8))
            .email(normalizedEmail != null ? normalizedEmail : source.getEmail())
            .build();
        if (source.getPhone1()!=null) {
            target.setPhone1(source.getPhone1());
        }
        if (source.getPhone2()!=null) {
            target.setPhone2(source.getPhone2());
        }
        return target;
    }

    @Override
    public Person applyChangesToExistingEntity(PersonPojo source, Person target) {
        throw new UnsupportedOperationException("This method is deprecated");
    }
}
