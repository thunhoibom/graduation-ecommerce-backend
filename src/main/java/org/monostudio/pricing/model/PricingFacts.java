package org.monostudio.pricing.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Read-only facts for evaluating promotion conditions (whitelist only).
 */
@Value
@Builder
public class PricingFacts {

    int cartSubtotal;
    int cartLineCount;
    int cartTotalUnits;
    /** Distinct product category ids present in the cart */
    Set<Long> cartCategoryIds;
    /** Distinct product barcodes present in the cart */
    Set<String> cartProductBarcodes;

    String userTier;
    int userMonthlySpendCents;

    boolean envIsHoliday;
    int envHourOfDay;
    LocalDateTime now;
}
