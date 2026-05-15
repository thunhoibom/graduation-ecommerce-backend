package org.monostudio.api.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.PersonPojo;
import org.monostudio.api.models.LoyaltyProfilePojo;
import org.monostudio.api.services.LoyaltyService;
import org.monostudio.api.services.ProfileService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Person;
import org.monostudio.jpa.entities.User;
import org.monostudio.jpa.exceptions.PersonNotFoundException;
import org.monostudio.jpa.exceptions.UserNotFoundException;
import org.monostudio.jpa.repositories.PeopleRepository;
import org.monostudio.jpa.repositories.UsersRepository;
import org.monostudio.jpa.services.conversion.PeopleConverterService;
import org.monostudio.jpa.services.crud.PeopleCrudService;
import org.monostudio.jpa.services.patch.PeoplePatchService;

import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;

@Service
public class ProfileServiceImpl
    implements ProfileService {
    private final UsersRepository usersRepository;
    private final PeopleCrudService peopleService;
    private final PeopleConverterService peopleConverter;
    private final PeoplePatchService peoplePatchService;
    private final PeopleRepository peopleRepository;
    private final LoyaltyService loyaltyService;

    @Autowired
    public ProfileServiceImpl(
        UsersRepository usersRepository,
        PeopleCrudService peopleService,
        PeopleConverterService peopleConverter,
        PeoplePatchService peoplePatchService,
        PeopleRepository peopleRepository,
        LoyaltyService loyaltyService
    ) {
        this.usersRepository = usersRepository;
        this.peopleService = peopleService;
        this.peopleConverter = peopleConverter;
        this.peoplePatchService = peoplePatchService;
        this.peopleRepository = peopleRepository;
        this.loyaltyService = loyaltyService;
    }

    @Override
    public PersonPojo getProfileFromUserName(String userName)
        throws UserNotFoundException, PersonNotFoundException {
        Optional<User> byName = usersRepository.findByName(userName);
        if (byName.isEmpty()) {
            throw new UserNotFoundException("There is no account with the specified username");
        }
        Person person = byName.get().getPerson();
        if (person==null) {
            throw new PersonNotFoundException("The account does not have an associated profile");
        } else {
            return peopleConverter.convertToPojo(person);
        }
    }

    @Override
    public LoyaltyProfilePojo getLoyaltyProfileFromUserName(String userName) throws EntityNotFoundException {
        return loyaltyService.getLoyaltyProfileFromUserName(userName);
    }

    @Transactional
    @Override
    public void updateProfileForUserWithName(String userName, PersonPojo profile)
        throws BadInputException, UserNotFoundException {
        User targetUser = this.getUserFromName(userName);
        Person target = targetUser.getPerson();
        if (target == null) {
            Optional<Person> existingProfile = peopleService.getExisting(profile);
            if (existingProfile.isPresent()) {
                Person person = existingProfile.get();

                // Check if this person is already associated with another account
                Optional<User> userWithEmail = usersRepository.findByPersonEmail(person.getEmail());
                Optional<User> userWithId = (person.getIdNumber() != null && !person.getIdNumber().isBlank())
                    ? usersRepository.findByPersonIdNumber(person.getIdNumber())
                    : Optional.empty();

                if (userWithEmail.isPresent() || userWithId.isPresent()) {
                    Long existingUserId = userWithEmail.isPresent() ? userWithEmail.get().getId() : userWithId.get().getId();
                    if (!existingUserId.equals(targetUser.getId())) {
                        throw new BadInputException("Person profile is associated to another account. Cannot use it.");
                    }
                }

                target = person;
                targetUser.setPerson(target);
                usersRepository.saveAndFlush(targetUser);
            } else {
                target = peopleConverter.convertToNewEntity(profile);
                target = peopleRepository.saveAndFlush(target);
                targetUser.setPerson(target);
                usersRepository.saveAndFlush(targetUser);
            }
        }
        target = peoplePatchService.patchExistingEntity(profile, target);
        peopleRepository.saveAndFlush(target);
    }

    private User getUserFromName(String userName)
        throws UserNotFoundException {
        Optional<User> userByName = usersRepository.findByName(userName);
        if (userByName.isEmpty()) {
            throw new UserNotFoundException("There is no account with the specified username");
        } else {
            return userByName.get();
        }
    }
}
