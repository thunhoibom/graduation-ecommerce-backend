package org.monostudio.shipping.ghn.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class GhnCreateOrderResult {
    String orderCode;
    String trackingNumber;
    String sortCode;
    Integer totalFee;
    Instant expectedDeliveryTime;
}
