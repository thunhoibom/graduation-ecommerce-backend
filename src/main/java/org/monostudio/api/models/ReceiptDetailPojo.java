package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude
public class ReceiptDetailPojo {
    private ProductPojo product;
    private String productName;
    private String variantSku;
    private int quantity;
    private int units;
    private int unitPrice;
    private Integer unitValue;
    private int lineTotal;
    private String description;
    private String imageUrl;

    // Compatibility aliases
    public void setUnits(int units) { this.quantity = units; this.units = units; }
    public void setUnitValue(Integer unitValue) { 
        this.unitPrice = unitValue != null ? unitValue : 0; 
        this.unitValue = unitValue; 
    }
}
