package org.monostudio.common.converters;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.monostudio.api.models.ReceiptPojo;
import org.monostudio.jpa.entities.Order;

@Component
public class OrderEntity2ReceiptPojo
    implements Converter<Order, ReceiptPojo> {

    @Override
    public ReceiptPojo convert(Order source) {
        ReceiptPojo target = new ReceiptPojo();
        target.setBuyOrder(source.getId());
        target.setDate(source.getDate());
        target.setTransportValue(source.getTransportValue());
        target.setTaxValue(source.getTaxesValue());
        target.setTotalItems(source.getTotalItems());
        target.setTotalValue(source.getTotalValue());
        target.setToken(source.getTransactionToken());
        return target;
    }
}
