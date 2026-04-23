package org.monostudio.api.models;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ShippingRateRequestContext {
    Integer subtotal;
    Double latitude;
    Double longitude;
    Integer toDistrictId;
    String toWardCode;
}
