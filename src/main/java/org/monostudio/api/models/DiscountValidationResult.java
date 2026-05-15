package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of a discount code validation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude
public class DiscountValidationResult {
    /** Whether the code was valid */
    private boolean valid;
    /** Discount amount in cents (0 if invalid) */
    private int discountAmount;
    /** Human-readable description or error message */
    private String message;
    /** The discount code (echoed back) */
    private String code;
    /** Discount type: PERCENTAGE | FIXED_AMOUNT | FREE_SHIPPING */
    private String type;
    /** Discount value for display (percentage or fixed amount in cents) */
    private Integer value;

    public static DiscountValidationResult invalid(String message) {
        return DiscountValidationResult.builder()
            .valid(false)
            .discountAmount(0)
            .message(message)
            .build();
    }

    public static DiscountValidationResult noDiscount() {
        return DiscountValidationResult.builder()
            .valid(true)
            .discountAmount(0)
            .message("No discount code provided")
            .build();
    }
}
