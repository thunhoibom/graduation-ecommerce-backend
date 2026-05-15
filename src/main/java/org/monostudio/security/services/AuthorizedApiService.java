package org.monostudio.security.services;

import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * Provides means to acknowledge the exact permissions of a user.
 */
public interface AuthorizedApiService {

    /**
     * Fetches the REST API routes that the current user can access.
     *
     * @param userDetails An object containing information about the current user.
     * @return A collection of REST API routes in String form.
     */
    Collection<String> getAuthorizedApiRoutes(UserDetails userDetails);

    /**
     * Fetches the operations that the current user can access on a specific REST API route.
     *
     * @param userDetails An object containing information about the current user.
     * @param apiRoute    The REST API route to ask about.
     * @return A collection of permissions in String form.
     */
    Collection<String> getAuthorizedApiRouteAccess(UserDetails userDetails, String apiRoute);
}
