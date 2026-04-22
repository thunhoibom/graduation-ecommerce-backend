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
public class FinanceReconciliationMismatchPojo {
    private String mismatchKey;
    private Long orderId;
    private String type;
    private String description;
    private String severity;
    private boolean resolved;
    private String resolutionNote;
    private String resolvedBy;
    private Instant resolvedAt;
}
