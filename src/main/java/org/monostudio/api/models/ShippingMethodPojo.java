package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude
public class ShippingMethodPojo {
    /** Always emit id in JSON so admin clients never build URLs with a missing id. */
    @JsonInclude(Include.ALWAYS)
    private Long id;
    private String name;
    private Integer baseFee;
    private Integer freeShippingThreshold;
    private Integer estimatedDaysMin;
    private Integer estimatedDaysMax;
    private Boolean active;
    private Integer pricePerKm;
    @Builder.Default
    private String carrierCode = "LOCAL";
    @Builder.Default
    private String rateMode = "DISTANCE";
    private String carrierServiceCode;
    private Long carrierShopId;
}
