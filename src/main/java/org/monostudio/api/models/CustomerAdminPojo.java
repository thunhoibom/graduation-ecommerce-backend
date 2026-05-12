package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 * Admin/customer-care view: customer id, person profile, orders, saved addresses, aggregates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class CustomerAdminPojo {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone1;
    private String phone2;
    private String idNumber;

    @Builder.Default
    private List<CustomerOrderSummaryPojo> orders = new ArrayList<>();

    private Integer orderCount;
    /** Sum of {@link org.monostudio.jpa.entities.Order#getTotalValue()} for this customer (same unit as order API). */
    private Long totalSpent;

    @Builder.Default
    private List<CustomerShippingAddressPojo> addresses = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(NON_NULL)
    public static class CustomerOrderSummaryPojo {
        private Long id;
        private String date;
        private Integer totalValue;
        private String status;
        private String paymentStatus;
        private Integer itemCount;
        private String recipientName;
        private String recipientPhone;
        private String shippingAddress;
        private String discountCode;
        private Integer discountValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(NON_NULL)
    public static class CustomerShippingAddressPojo {
        private Long id;
        private String name;
        private String phone;
        private String email;
        private String address1;
        private String address2;
        private String ward;
        private String district;
        private String city;
        private String country;
        private Boolean isDefault;
        private String label;
    }
}
