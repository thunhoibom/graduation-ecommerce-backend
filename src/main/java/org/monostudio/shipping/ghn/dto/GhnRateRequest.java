package org.monostudio.shipping.ghn.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GhnRateRequest {
    long shopId;
    Integer serviceId;
    Integer serviceTypeId;
    int insuranceValue;
    int fromDistrictId;
    String fromWardCode;
    int toDistrictId;
    String toWardCode;
    int weight;
    int length;
    int width;
    int height;
}
