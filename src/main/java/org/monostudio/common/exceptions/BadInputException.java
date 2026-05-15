package org.monostudio.common.exceptions;

/**
 * Thrown by most services that validate data as inputted by the consumer of the application's REST API
 */
public class BadInputException
    extends Exception {

    public BadInputException(String message) {
        super(message);
    }

    public BadInputException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadInputException(Throwable cause) {
        super(cause);
    }
}
