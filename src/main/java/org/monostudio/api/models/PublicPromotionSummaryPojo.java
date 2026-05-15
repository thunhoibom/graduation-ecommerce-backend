package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.monostudio.jpa.entities.PromotionScope;

import java.time.LocalDateTime;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class PublicPromotionSummaryPojo {

    private Long id;
    private String name;
    /** CART, SHIPPING, PRODUCT, CATEGORY */
    private PromotionScope scope;
    /** Giảm giá / freeship — mô tả ngắn */
    private String effectsSummary;
    /** Điều kiện áp dụng (đã làm dễ đọc) */
    private String conditionsSummary;
    /** True khi scope PRODUCT/CATEGORY — giá đã phản ánh trên trang sản phẩm */
    private Boolean reflectedInProductPrice;
    private LocalDateTime activeUntil;
}
