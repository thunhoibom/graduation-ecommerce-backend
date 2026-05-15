package org.monostudio.api.services.impl;

import com.querydsl.core.types.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.PersonPojo;
import org.monostudio.api.models.RegistrationPojo;
import org.monostudio.api.services.RegistrationService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.api.services.CustomerIdentityService;
import org.monostudio.common.EmailNormalizer;
import org.monostudio.jpa.entities.Person;
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
import java.util.UUID;

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
    private final CustomerIdentityService customerIdentityService;

    @Autowired
    public RegistrationServiceImpl(
        PeopleRepository peopleRepository,
        UsersRepository usersRepository,
        UserRolesRepository rolesRepository,
        CustomersRepository customersRepository,
        PasswordEncoder passwordEncoder,
        PeopleConverterService peopleConverterService,
        CustomerIdentityService customerIdentityService
    ) {
        this.peopleRepository = peopleRepository;
        this.usersRepository = usersRepository;
        this.rolesRepository = rolesRepository;
        this.customersRepository = customersRepository;
        this.passwordEncoder = passwordEncoder;
        this.peopleConverterService = peopleConverterService;
        this.customerIdentityService = customerIdentityService;
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
        String email = EmailNormalizer.normalize(sourcePerson.getEmail());
        if (email != null) {
            sourcePerson.setEmail(email);
        }
        String idNumber = sourcePerson.getIdNumber();

        if (email != null && usersRepository.findByPersonEmailIgnoreCase(email).isPresent()) {
            throw new EntityExistsException("An account with this email already exists.");
        }

        // Check idNumber uniqueness
        if (idNumber != null && !idNumber.isBlank()) {
            if (usersRepository.findByPersonIdNumber(idNumber).isPresent()) {
                throw new EntityExistsException("That ID number is already registered and associated to an account.");
            }
        }

        // Find existing person to reuse (from guest orders) or create new one
        Person personToUse;
        Optional<Person> existingPerson = Optional.empty();
        if (email != null) {
            existingPerson = peopleRepository.findByEmailIgnoreCase(email);
        }
        if (existingPerson.isEmpty() && idNumber != null && !idNumber.isBlank()) {
            existingPerson = peopleRepository.findByIdNumber(idNumber);
        }

        if (existingPerson.isPresent()) {
            personToUse = existingPerson.get();
            // Optional: update person info if needed
        } else {
            personToUse = peopleConverterService.convertToNewEntity(sourcePerson);
            personToUse = peopleRepository.saveAndFlush(personToUse);
        }

        User newUser = this.convertToUser(registration);
        newUser.setPerson(personToUse);
        usersRepository.saveAndFlush(newUser);
        // Credential info not logged — security best practice

        // Ensure Customer record exists for this person
        ensureCustomerProfile(personToUse);
    }

    @Override
    @Transactional
    public User findOrCreateGoogleUser(String email, String givenName, String familyName, String fallbackKey)
        throws BadInputException {
        String normalizedEmail = EmailNormalizer.normalize(email);
        if (normalizedEmail == null) {
            throw new BadInputException("Google account email is missing.");
        }

        Optional<User> existingUser = usersRepository.findByPersonEmailIgnoreCase(normalizedEmail);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        Person person = peopleRepository.findByEmailIgnoreCase(normalizedEmail)
            .orElseGet(() -> peopleRepository.saveAndFlush(Person.builder()
                .firstName(defaultIfBlank(givenName, "Google"))
                .lastName(defaultIfBlank(familyName, "User"))
                .email(normalizedEmail)
                // National ID optional; use null when absent (requires DB column nullable — see database-migrations §13).
                .idNumber(null)
                .phone1("")
                .phone2("")
                .build()));

        User newUser = User.builder()
            .name(generateUniqueUsername(normalizedEmail, fallbackKey))
            .password(passwordEncoder.encode(UUID.randomUUID().toString()))
            .person(person)
            .build();
        newUser.setUserRole(resolveCustomerRole());
        User savedUser = usersRepository.saveAndFlush(newUser);
        ensureCustomerProfile(person);
        return savedUser;
    }

    protected User convertToUser(RegistrationPojo registration) {
        String password = passwordEncoder.encode(registration.getPassword());
        User target = User.builder()
            .name(registration.getName())
            .password(password)
            .build();

        target.setUserRole(resolveCustomerRole());
        return target;
    }

    private UserRole resolveCustomerRole() {
        Optional<UserRole> customerRole = rolesRepository.findByName("Customer");
        if (customerRole.isEmpty()) {
            throw new IllegalStateException("No user role matches 'Customer', database might be compromised");
        }
        return customerRole.get();
    }

    private void ensureCustomerProfile(Person person) {
        customerIdentityService.ensureCustomerForPerson(person);
    }

    private String generateUniqueUsername(String email, String fallbackKey) {
        String emailPrefix = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        String base = (emailPrefix + "_google")
            .toLowerCase()
            .replaceAll("[^a-z0-9._-]", "_");
        if (base.isBlank()) {
            base = "google_user";
        }
        String candidate = base;
        int suffix = 1;
        while (usersRepository.findByName(candidate).isPresent()) {
            if (fallbackKey != null && !fallbackKey.isBlank() && suffix == 1) {
                candidate = base + "_" + fallbackKey.substring(0, Math.min(8, fallbackKey.length())).toLowerCase();
            } else {
                candidate = base + "_" + suffix;
                suffix++;
            }
        }
        return candidate;
    }

    private String defaultIfBlank(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
