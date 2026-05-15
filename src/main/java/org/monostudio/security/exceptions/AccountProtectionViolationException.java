package org.monostudio.security.exceptions;

/**
 * Thrown when an attempt to destroy a protected account is made.
 */
public class AccountProtectionViolationException
    extends RuntimeException {

    public AccountProtectionViolationException(String message) {
        super(message);
    }
}
