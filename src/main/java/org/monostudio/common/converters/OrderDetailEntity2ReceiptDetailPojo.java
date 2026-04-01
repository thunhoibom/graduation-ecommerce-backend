package org.monostudio.common.converters;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.monostudio.api.models.ReceiptDetailPojo;
import org.monostudio.jpa.entities.OrderDetail;

@Component
public class OrderDetailEntity2ReceiptDetailPojo
    implements Converter<OrderDetail, ReceiptDetailPojo> {

    @Override
    public ReceiptDetailPojo convert(OrderDetail source) {
        ReceiptDetailPojo target = new ReceiptDetailPojo();
        target.setUnits(source.getUnits());
        target.setUnitValue(source.getUnitValue());
        target.setDescription(source.getDescription());
        return target;
    }
}
