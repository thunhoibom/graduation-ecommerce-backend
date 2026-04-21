package org.monostudio.shipping.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentTrackingEvent {
    private Long trackingId;
    private Long orderId;
    private String trackingNumber;
    private String shipperCode;
    private String status;
    private String location;
    private String description;
    private Instant eventTime;
}
