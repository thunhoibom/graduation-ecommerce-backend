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
public class ShipmentTrackingWebhookPayload {
    private String shipper_code;
    private String tracking_number;
    private Long order_id;
    private String status;
    private String location;
    private String description;
    private Instant event_time;
}
