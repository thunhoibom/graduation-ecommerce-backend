package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.Instant;
import java.util.Collection;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude
public class OrderPojo {
    private Long id;
    private Long buyOrder;
    @JsonIgnore
    private String token;
    private String cartSessionToken;
    private Instant date;
    @Valid
    @NotEmpty
    @JsonInclude(NON_EMPTY)
    private Collection<OrderDetailPojo> details;
    private int netValue;
    private int taxValue;
    private int transportValue;
    private int totalValue;
    private int totalItems;
    private int totalRefundedAmount;
    private String discountCode;
    private int discountValue;
    private String status;
    private String paymentStatus;
    private String billingType;
    @NotBlank
    private String paymentType;
    @Valid
    private PersonPojo customer;
    private PersonPojo salesperson;
    private String shipper;
    private BillingCompanyPojo billingCompany;
    private AddressPojo billingAddress;
    private AddressPojo shippingAddress;
    private String customerName;
    private String customerEmail;
    private String recipientName;
    private String recipientPhone;
    private String recipientEmail;
}
