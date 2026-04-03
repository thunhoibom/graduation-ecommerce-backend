package org.monostudio.api.services;

import org.monostudio.api.models.DiscountValidationResult;
import org.monostudio.common.exceptions.BadInputException;

/**
 * Service for validating and applying discount codes during checkout.
 */
public interface DiscountService {

    /**
     * Validates a discount code against the given subtotal.
     *
     * @param code     The discount code string (may be null/blank — returns no discount)
     * @param subtotal The cart subtotal in cents before discount
     * @return A DiscountValidationResult with the discount amount and description
     */
    DiscountValidationResult validateDiscount(String code, int subtotal);

    /**
     * Validates and redeems (increments use count) a discount code.
     * Call this only after payment is confirmed.
     *
     * @param code       The discount code string
     * @param subtotal   The cart subtotal in cents
     * @param customerId Optional customer ID for per-customer limit tracking
     * @throws BadInputException if the code is invalid or cannot be redeemed
     */
    void redeemDiscount(String code, int subtotal, Long customerId) throws BadInputException;
}
