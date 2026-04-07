package org.monostudio.api.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result from a payment gateway commit (payment confirmation) call.
 * Carries both the response code and the authorized amount for verification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResultPojo {
    /**
     * Gateway response code. 0 = success (Webpay Plus).
     */
    private int responseCode;

    /**
     * The amount actually authorized / charged by the payment gateway, in cents.
     * Used to verify it matches the order total before marking the order as paid.
     */
    private int authorizedAmount;
}
