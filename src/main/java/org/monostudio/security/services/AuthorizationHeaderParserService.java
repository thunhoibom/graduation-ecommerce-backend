package org.monostudio.security.services;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;

/**
 * Interface for parsing Authorization headers into tokens
 *
 * @param <T> The token body type
 */
public interface AuthorizationHeaderParserService<T> {

    /**
     * Extracts the Authorization header value from a map of http headers
     *
     * @param httpHeaders The http headers
     * @return The value for the Authorization header, or null if not found
     */
    @Nullable
    String extractAuthorizationHeader(HttpHeaders httpHeaders);

    /**
     * @param token The raw token
     * @return The expected token class
     */
    T parseToken(String token) throws IllegalStateException;
}
