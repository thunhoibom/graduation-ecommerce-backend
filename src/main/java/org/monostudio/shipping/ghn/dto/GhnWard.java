package org.monostudio.shipping.ghn.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GhnWard {
    String wardCode;
    int districtId;
    String wardName;
}
