package org.monostudio.api.services;

import org.monostudio.api.models.CheckoutOtpInitiateResponse;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Order;

public interface CheckoutOtpService {
    CheckoutOtpInitiateResponse issueOtpForOrder(Order order) throws BadInputException;

    CheckoutOtpInitiateResponse resendOtpForOrder(Long orderId) throws BadInputException;

    void verifyOtp(Long orderId, String otpCode) throws BadInputException;
}
