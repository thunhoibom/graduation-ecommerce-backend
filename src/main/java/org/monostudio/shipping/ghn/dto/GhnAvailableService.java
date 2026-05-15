package org.monostudio.shipping.ghn.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GhnAvailableService {
    Integer serviceId;
    Integer serviceTypeId;
    String shortName;
}
