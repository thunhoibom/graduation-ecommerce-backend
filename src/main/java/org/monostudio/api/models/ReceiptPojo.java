package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.Instant;
import java.util.Collection;

@Data
@JsonInclude
public class ReceiptPojo {
    private long buyOrder;
    private Collection<ReceiptDetailPojo> details;
    private Instant date;
    private String status;
    private String token;
    private int totalValue;
    private int taxValue;
    private int transportValue;
    private int totalItems;
}
