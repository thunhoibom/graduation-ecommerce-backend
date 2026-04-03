package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

/**
 * Request DTO for POST /public/checkout/start.
 * Represents the cart contents and checkout choices needed to start a checkout session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude
public class CheckoutStartRequest {
    /** The cart session token (from CartSession.token). Required. */
    @NotBlank
    private String sessionToken;

    /** The selected shipping method ID. */
    @NotNull
    private Long shippingMethodId;

    /** Optional discount code to apply. */
    private String discountCode;

    /** Customer information (used to create or match a Customer record). */
    @Valid
    @NotNull
    private PersonPojo customer;

    /** Shipping address for the order. */
    private AddressPojo shippingAddress;

    /** Payment type name (e.g. "Webpay Plus"). */
    @NotBlank
    private String paymentType;

    /** Billing type name (e.g. "enterprise", "individual"). */
    @NotBlank
    private String billingType;

    /** Required when billingType is "enterprise". */
    private BillingCompanyPojo billingCompany;
    /** Required when billingType is "enterprise". */
    private AddressPojo billingAddress;
}
