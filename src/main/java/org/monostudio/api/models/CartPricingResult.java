package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class CartPricingResult {

    private Integer subtotal;
    private Integer discountAmount;
    private Integer totalAfterDiscount;
    private Boolean freeShipping;
    private List<AppliedPromotionLine> appliedPromotions;
    /** Gợi ý khi giỏ gần đạt ngưỡng subtotal (MVP: chỉ điều kiện cart.subtotal GTE/GT) */
    private List<PromotionNearMissPojo> promotionNearMisses;
    private String appliedDiscountCode;
}
