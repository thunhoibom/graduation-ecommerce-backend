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
public class FinanceRefundOperationPojo {
    private Long returnRequestId;
    private Long orderId;
    private String status;
    private Integer refundAmount;
    private int refundedAmountToDate;
    private String refundMethod;
    private String reason;
    private String adminNotes;
    private Instant lastModified;
}
