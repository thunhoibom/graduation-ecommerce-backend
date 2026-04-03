package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class DiscountCodePojo {

    private Long id;

    /** The code as typed by the customer (case-insensitive) */
    @NotBlank
    private String code;

    /** Description shown at checkout */
    private String description;

    /**
     * Discount type: PERCENTAGE | FIXED_AMOUNT | FREE_SHIPPING
     */
    @NotNull
    private String type;

    /**
     * For PERCENTAGE: 0.01–100.00
     * For FIXED_AMOUNT: value in cents
     */
    @NotNull
    private Integer value;

    /** Maximum total redemptions (null = unlimited) */
    private Integer maxUses;

    /** Current total redemption count */
    private Integer useCount;

    /** Maximum uses per customer (null = unlimited) */
    private Integer maxUsesPerCustomer;

    /** Minimum cart subtotal required (in cents, null = no minimum) */
    private Integer minCartValue;

    /** Validity start */
    private LocalDateTime validFrom;

    /** Validity end */
    private LocalDateTime validUntil;

    @Builder.Default
    private Boolean active = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ─── Read-only derived fields ────────────────────────────────────────────

    /** Whether the code is currently within its validity window */
    private Boolean currentlyValid;

    /** Remaining uses (maxUses - useCount), null if unlimited */
    private Integer remainingUses;
}
