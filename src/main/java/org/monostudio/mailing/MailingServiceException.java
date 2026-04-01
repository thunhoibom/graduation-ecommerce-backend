package org.monostudio.mailing;

public class MailingServiceException
    extends Exception {

    public MailingServiceException(String string) {
        super(string);
    }

    public MailingServiceException(String string, Throwable throwable) {
        super(string, throwable);
    }
}
