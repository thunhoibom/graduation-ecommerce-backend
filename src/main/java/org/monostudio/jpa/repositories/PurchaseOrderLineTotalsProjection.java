package org.monostudio.jpa.repositories;

public interface PurchaseOrderLineTotalsProjection {
    Long getPurchaseOrderId();

    Long getLineCount();

    Long getOrderedTotalAmount();

    Long getReceivedTotalAmount();
}
