package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude
public class ReceiptDetailPojo {
    private ProductPojo product;
    private int units;
    private Integer unitValue;
    private String description;
}
