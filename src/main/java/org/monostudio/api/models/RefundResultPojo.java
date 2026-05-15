package org.monostudio.api.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result from a payment gateway refund call.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResultPojo {
    /**
     * True if the gateway accepted the refund.
     */
    private boolean success;

    /**
     * Gateway response code. 0 = success.
     */
    private int responseCode;

    /**
     * Refund type from the gateway: "REVERSED" (full) or "NULLIFIED" (void before EOD).
     */
    private String type;

    /**
     * Remaining balance on the card after the refund.
     */
    private Long balance;

    /**
     * Human-readable error message when success == false.
     */
    private String errorMessage;
}
