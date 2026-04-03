package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response Pojo returned by GET /public/shipping/methods?subtotal={vnd}.
 * Contains the method details plus the computed fee for the given subtotal.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude
public class ShippingRatePojo {
    private Long id;
    private String name;
    private int fee;
    private int estimatedDaysMin;
    private int estimatedDaysMax;
    private boolean freeShipping;
}
