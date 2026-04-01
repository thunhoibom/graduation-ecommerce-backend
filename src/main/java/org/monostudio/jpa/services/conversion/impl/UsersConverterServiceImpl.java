package org.monostudio.jpa.services.conversion.impl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.PersonPojo;
import org.monostudio.api.models.UserPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Person;
import org.monostudio.jpa.entities.User;
import org.monostudio.jpa.entities.UserRole;
import org.monostudio.jpa.repositories.PeopleRepository;
import org.monostudio.jpa.repositories.UserRolesRepository;
import org.monostudio.jpa.services.conversion.PeopleConverterService;
import org.monostudio.jpa.services.conversion.UsersConverterService;

import java.util.Optional;

@Transactional
@Service
public class UsersConverterServiceImpl
    implements UsersConverterService {
    private final Logger logger = LoggerFactory.getLogger(UsersConverterServiceImpl.class);
    private final UserRolesRepository rolesRepository;
    private final PeopleConverterService peopleConverterService;
    private final PeopleRepository peopleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UsersConverterServiceImpl(
        UserRolesRepository rolesRepository,
        PeopleConverterService peopleConverterService,
        PeopleRepository peopleRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.rolesRepository = rolesRepository;
        this.peopleConverterService = peopleConverterService;
        this.peopleRepository = peopleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserPojo convertToPojo(User source) {
        UserPojo target = UserPojo.builder()
            .name(source.getName())
            .role(source.getUserRole().getName())
            .build();
        Person sourcePerson = source.getPerson();
        if (sourcePerson!=null) {
            PersonPojo personPojo = peopleConverterService.convertToPojo(sourcePerson);
            target.setPerson(personPojo);
        }
        return target;
    }

    @Override
    public User convertToNewEntity(UserPojo source) throws BadInputException {
        User target = User.builder()
            .name(source.getName())
            .build();

        String sourceRole = source.getRole();
        if (StringUtils.isBlank(sourceRole)) {
            throw new BadInputException("The user was not given any role");
        }
        Optional<UserRole> roleByName = rolesRepository.findByName(sourceRole);
        if (roleByName.isPresent()) {
            logger.trace("User role found");
            target.setUserRole(roleByName.get());
        } else {
            throw new BadInputException("The specified user role does not exist");
        }

        PersonPojo sourcePerson = source.getPerson();
        if (sourcePerson!=null && StringUtils.isNotBlank(sourcePerson.getIdNumber())) {
            logger.trace("Finding person profile...");
            Optional<Person> personByIdNumber = peopleRepository.findByIdNumber(sourcePerson.getIdNumber());
            if (personByIdNumber.isPresent()) {
                logger.trace("Person profile found");
                target.setPerson(personByIdNumber.get());
            }
        }

        if (StringUtils.isNotBlank(source.getPassword())) {
            String rawPassword = source.getPassword();
            String encodedPassword = passwordEncoder.encode(rawPassword);
            target.setPassword(encodedPassword);
        }
        return target;
    }

    @Override
    public User applyChangesToExistingEntity(UserPojo source, User target) {
        throw new UnsupportedOperationException("This method is deprecated");
    }
}
