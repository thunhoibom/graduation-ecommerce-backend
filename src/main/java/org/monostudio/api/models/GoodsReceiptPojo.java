package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
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
public class GoodsReceiptPojo {
    private Long id;
    private String code;
    private Long purchaseOrderId;
    private String purchaseOrderCode;
    private Long receivedBy;
    private String note;
    private String createdAt;
    private List<GoodsReceiptLinePojo> lines;
}
