package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class AppliedPromotionLine {

    private Long promotionRuleId;
    private String name;
    private Integer discountAmount;
    private Boolean freeShipping;
    /** Present when this line comes from a coupon code */
    private String couponCode;
}
