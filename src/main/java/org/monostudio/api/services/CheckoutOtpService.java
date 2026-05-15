package org.monostudio.api.services;

import org.monostudio.api.models.CheckoutOtpInitiateResponse;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Order;

public interface CheckoutOtpService {
    /**
     * @param checkoutEmail email the shopper entered on checkout; OTP must not fall back to account profile email.
     */
    CheckoutOtpInitiateResponse issueOtpForOrder(Order order, String checkoutEmail) throws BadInputException;

    CheckoutOtpInitiateResponse resendOtpForOrder(Long orderId, String checkoutEmail) throws BadInputException;

    void verifyOtp(Long orderId, String otpCode) throws BadInputException;
}
