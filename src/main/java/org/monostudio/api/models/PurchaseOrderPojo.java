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
public class PurchaseOrderPojo {
    private Long id;
    private String code;
    private String status;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private String expectedDate;
    private String submittedAt;
    private String approvedAt;
    private String receivedAt;
    private Long requestedBy;
    private Long approvedBy;
    private String warehouseId;
    private String locationCode;
    private String note;
    private String createdAt;
    private String updatedAt;
    private Integer lineCount;
    private Long orderedTotalAmount;
    private Long receivedTotalAmount;
    private List<PurchaseOrderLinePojo> lines;
}
