package org.monostudio.common.services;

/**
 * Compiles once, then memoizes common and important regex Patterns
 * such as id numbers from people and companies
 */
public interface RegexMatcherAdapterService {
    boolean isAValidIdNumber(String matchAgainst);

    boolean isAValidAuthorizationHeader(String matchAgainst);
}
