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
public class PromotionNearMissPojo {

    private Long ruleId;
    private String title;
    /** Thông báo hiển thị cho khách (tiếng Việt) */
    private String messageVi;
    /** Số tiền còn thiếu so với ngưỡng (VND, đơn vị giống cart.subtotal) */
    private Integer remainingAmount;
}
