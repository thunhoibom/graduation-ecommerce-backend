package org.monostudio.security.services;

import org.monostudio.jpa.entities.Permission;
import org.monostudio.jpa.entities.User;

import java.util.Set;

/**
 * Provides a mean to acknowledge the raw permissions data as stored in the persistence context.
 */
public interface UserPermissionsService {

    /**
     * Fetches the {@link java.util.Set} of unique permissions associated to a given user.
     *
     * @param user The user to fetch permissions for.
     * @return A Set of permission entities.
     */
    Set<Permission> loadPermissionsForUser(User user);
}
