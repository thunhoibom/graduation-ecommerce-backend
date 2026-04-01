package org.monostudio.api.services.impl;

import com.querydsl.core.types.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.PersonPojo;
import org.monostudio.api.models.RegistrationPojo;
import org.monostudio.api.services.RegistrationService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Customer;
import org.monostudio.jpa.entities.Person;
import org.monostudio.jpa.entities.QPerson;
import org.monostudio.jpa.entities.QUser;
import org.monostudio.jpa.entities.User;
import org.monostudio.jpa.entities.UserRole;
import org.monostudio.jpa.repositories.CustomersRepository;
import org.monostudio.jpa.repositories.PeopleRepository;
import org.monostudio.jpa.repositories.UserRolesRepository;
import org.monostudio.jpa.repositories.UsersRepository;
import org.monostudio.jpa.services.conversion.PeopleConverterService;

import jakarta.persistence.EntityExistsException;
import java.util.Optional;

@Service
public class RegistrationServiceImpl
    implements RegistrationService {
    private final Logger logger = LoggerFactory.getLogger(RegistrationServiceImpl.class);
    private final PeopleRepository peopleRepository;
    private final UsersRepository usersRepository;
    private final UserRolesRepository rolesRepository;
    private final CustomersRepository customersRepository;
    private final PasswordEncoder passwordEncoder;
    private final PeopleConverterService peopleConverterService;

    @Autowired
    public RegistrationServiceImpl(
        PeopleRepository peopleRepository,
        UsersRepository usersRepository,
        UserRolesRepository rolesRepository,
        CustomersRepository customersRepository,
        PasswordEncoder passwordEncoder,
        PeopleConverterService peopleConverterService
    ) {
        this.peopleRepository = peopleRepository;
        this.usersRepository = usersRepository;
        this.rolesRepository = rolesRepository;
        this.customersRepository = customersRepository;
        this.passwordEncoder = passwordEncoder;
        this.peopleConverterService = peopleConverterService;
    }

    @Override
    public void register(RegistrationPojo registration)
        throws BadInputException, EntityExistsException {
        String username = registration.getName();
        Predicate userWithSameName = QUser.user.name.eq(username);
        if (usersRepository.exists(userWithSameName)) {
            throw new EntityExistsException("That username is taken.");
        }

        PersonPojo sourcePerson = registration.getProfile();
        Person newPerson = peopleConverterService.convertToNewEntity(sourcePerson);

        Predicate sameProfileData = QPerson.person.idNumber.eq(sourcePerson.getIdNumber());
        if (peopleRepository.exists(sameProfileData)) {
            throw new EntityExistsException("That ID number is already registered and associated to an account.");
        } else {
            newPerson = peopleRepository.saveAndFlush(newPerson);
        }

        User newUser = this.convertToUser(registration);
        newUser.setPerson(newPerson);
        usersRepository.saveAndFlush(newUser);
        logger.info("New user created with name '{}' and idNumber '{}'", newUser.getName(), newPerson.getIdNumber());

        Customer newCustomer = Customer.builder()
            .person(newPerson)
            .build();
        customersRepository.saveAndFlush(newCustomer);
    }

    protected User convertToUser(RegistrationPojo registration) {
        String password = passwordEncoder.encode(registration.getPassword());
        User target = User.builder()
            .name(registration.getName())
            .password(password)
            .build();

        Optional<UserRole> customerRole = rolesRepository.findByName("Customer");
        if (customerRole.isEmpty()) {
            throw new IllegalStateException("No user role matches 'Customer', database might be compromised");
        } else {
            target.setUserRole(customerRole.get());
        }
        return target;
    }
}
