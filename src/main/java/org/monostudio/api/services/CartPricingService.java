package org.monostudio.api.services;

import org.monostudio.api.models.CartPricingResult;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.CartSession;

public interface CartPricingService {

    /**
     * Computes promotions for the cart, optionally persists snapshot on the session, and returns breakdown.
     *
     * @param sessionToken cart session token (header value)
     * @param couponCode   optional coupon typed by customer
     * @param persist      when true, writes snapshot fields on {@link CartSession}
     */
    CartPricingResult calculate(String sessionToken, String couponCode, boolean persist) throws BadInputException;

    /**
     * Same pricing logic for checkout (server-side); does not persist cart snapshot.
     */
    CartPricingResult calculateForSession(CartSession cart, String couponCode, Long customerId) throws BadInputException;
}
