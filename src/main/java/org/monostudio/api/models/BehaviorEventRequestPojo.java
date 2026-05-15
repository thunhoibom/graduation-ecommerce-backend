package org.monostudio.api.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BehaviorEventRequestPojo {
    private String deviceId;
    private Long customerId;
    private String eventType;
    private Map<String, Object> payload;
}
