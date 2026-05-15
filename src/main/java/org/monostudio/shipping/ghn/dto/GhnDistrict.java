package org.monostudio.shipping.ghn.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GhnDistrict {
    int districtId;
    int provinceId;
    String districtName;
}
