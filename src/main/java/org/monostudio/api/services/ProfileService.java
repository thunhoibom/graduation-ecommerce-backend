package org.monostudio.api.services;

import org.monostudio.api.models.PersonPojo;
import org.monostudio.api.models.LoyaltyProfilePojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.exceptions.UserNotFoundException;

import jakarta.persistence.EntityNotFoundException;

/**
 * Provides means for users to retrieve and update their stored personal information.
 */
public interface ProfileService {

    /**
     * Fetches personal information from a given username in a wrapper
     * {@link org.monostudio.api.models.PersonPojo} object.
     *
     * @param userName The name of the user to fetch data from.
     * @return The personal information of the user.
     * @throws EntityNotFoundException When no user with the provided name exists.
     */
    PersonPojo getProfileFromUserName(String userName) throws EntityNotFoundException;

    LoyaltyProfilePojo getLoyaltyProfileFromUserName(String userName) throws EntityNotFoundException;

    /**
     * Updates personal information for a given user.
     *
     * @param userName The name of the user to fetch data from.
     * @return The personal information of the user.
     * @throws EntityNotFoundException When no user with the provided name exists.
     */
    void updateProfileForUserWithName(String userName, PersonPojo profile) throws BadInputException, UserNotFoundException;
}
