package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.Instant;
import java.util.Collection;

@Data
@JsonInclude
public class ReceiptPojo {
    private long buyOrder;
    private Collection<ReceiptDetailPojo> items;
    private Instant createdAt;
    private String status;
    private String token;
    private String paymentType;
    private int total;
    private int subtotal;
    private int taxValue;
    private int shippingFee;
    private int discountAmount;
    private int totalItems;
    private String customerName;
    private String customerEmail;
    private AddressPojo shippingAddress;
    
    // Compatibility aliases
    public void setDetails(Collection<ReceiptDetailPojo> details) { this.items = details; }
    public Collection<ReceiptDetailPojo> getDetails() { return this.items; }
    public void setDate(Instant date) { this.createdAt = date; }
    public Instant getDate() { return this.createdAt; }
    public void setTotalValue(int totalValue) { this.total = totalValue; }
    public int getTotalValue() { return this.total; }
    public void setTransportValue(int transportValue) { this.shippingFee = transportValue; }
    public int getTransportValue() { return this.shippingFee; }
}
