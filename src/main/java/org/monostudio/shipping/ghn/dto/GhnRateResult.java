package org.monostudio.shipping.ghn.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GhnRateResult {
    int totalFee;
    String source;
}
