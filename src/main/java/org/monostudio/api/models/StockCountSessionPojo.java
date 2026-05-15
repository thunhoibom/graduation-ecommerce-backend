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
public class StockCountSessionPojo {
    private Long id;
    private String code;
    private String status;
    private String warehouseId;
    private String locationCode;
    private String plannedAt;
    private String countedAt;
    private String approvedAt;
    private String postedAt;
    private Long requestedBy;
    private Long approvedBy;
    private String note;
    private String createdAt;
    private String updatedAt;
    private List<StockCountLinePojo> lines;
}
