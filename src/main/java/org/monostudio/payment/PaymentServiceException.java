package org.monostudio.payment;

public class PaymentServiceException
    extends Exception {

    public PaymentServiceException(String string) {
        super(string);
    }

    public PaymentServiceException(String string, Throwable thrwbl) {
        super(string, thrwbl);
    }
}
