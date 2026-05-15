package org.monostudio.api.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceReconciliationSummaryPojo {
    private long grossPaidAmount;
    private long refundAmount;
    private long netAmount;
    private long failedPaymentCount;
    private long unresolvedMismatchCount;
}
