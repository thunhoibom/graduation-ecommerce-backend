package org.monostudio.api.services;

import org.monostudio.api.models.RegistrationPojo;
import org.monostudio.common.exceptions.BadInputException;

import jakarta.persistence.EntityExistsException;

/**
 * Allows anonymous consumers of the REST API to create user accounts.
 */
public interface RegistrationService {

    /**
     * Core registration mechanism. Takes the registration request and fulfills it.
     *
     * @param registration An object containing the details of the registration request.
     * @throws BadInputException     When the registration request contains invalid data.
     * @throws EntityExistsException When a user account matching the provided details already exists.
     */
    void register(RegistrationPojo registration) throws BadInputException, EntityExistsException;
}
