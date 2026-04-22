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
public class FinanceRefundRetryItemPojo {
    private Long queueId;
    private Long orderId;
    private String transactionToken;
    private int amount;
    private String reason;
    private String status;
    private int attemptCount;
    private int failedAttempts;
    private Instant nextRetryAt;
    private String lastError;
    private Instant updatedAt;
}
