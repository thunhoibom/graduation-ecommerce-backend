package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    private Long id;

    @NotBlank
    private String name;

    @NotNull
    @Min(0)
    private Integer baseFee;

    @Min(0)
    private Integer freeShippingThreshold;

    @NotNull
    @Min(1)
    private Integer estimatedDaysMin;

    @NotNull
    @Min(1)
    private Integer estimatedDaysMax;

    @NotNull
    private Boolean active;

    @Min(0)
    private Integer pricePerKm;

    @Builder.Default
    private String carrierCode = "LOCAL";

    @Builder.Default
    private String rateMode = "DISTANCE";

    private String carrierServiceCode;

    private Long carrierShopId;
}
