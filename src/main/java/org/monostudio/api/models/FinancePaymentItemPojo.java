package org.monostudio.api.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancePaymentItemPojo {
    private Long orderId;
    private String transactionToken;
    private String gateway;
    private String orderStatus;
    private String paymentStatus;
    private int orderTotal;
    private String callbackResult;
    private Integer callbackAuthorizedAmount;
    private Instant processedAt;
}
