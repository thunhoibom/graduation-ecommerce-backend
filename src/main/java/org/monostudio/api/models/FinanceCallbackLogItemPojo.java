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
public class FinanceCallbackLogItemPojo {
    private Long id;
    private Long orderId;
    private String token;
    private String result;
    private String orderStatusAfter;
    private Integer authorizedAmount;
    private Instant processedAt;
}
