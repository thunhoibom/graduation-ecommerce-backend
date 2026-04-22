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
public class StockTransferPojo {
    private Long id;
    private String code;
    private String status;
    private String warehouseId;
    private String fromLocation;
    private String toLocation;
    private Long requestedBy;
    private Long approvedBy;
    private String submittedAt;
    private String approvedAt;
    private String completedAt;
    private String note;
    private String createdAt;
    private String updatedAt;
    private List<StockTransferLinePojo> lines;
}
