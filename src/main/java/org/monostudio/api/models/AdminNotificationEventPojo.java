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
public class AdminNotificationEventPojo {
    private String eventId;
    private String type;
    private Long orderId;
    private String orderCode;
    private Instant createdAt;
    private Integer totalAmount;
    private String status;
}
