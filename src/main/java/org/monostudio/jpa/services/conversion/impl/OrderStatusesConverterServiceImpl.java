package org.monostudio.jpa.services.conversion.impl;

import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.OrderStatusPojo;
import org.monostudio.jpa.entities.OrderStatus;
import org.monostudio.jpa.services.conversion.OrderStatusesConverterService;

@Service
@NoArgsConstructor
public class OrderStatusesConverterServiceImpl
    implements OrderStatusesConverterService {

    @Override
    public OrderStatusPojo convertToPojo(OrderStatus source) {
        return OrderStatusPojo.builder()
            .code(source.getCode())
            .name(source.getName())
            .build();
    }

    @Override
    public OrderStatus convertToNewEntity(OrderStatusPojo source) {
        return OrderStatus.builder()
            .code(source.getCode())
            .name(source.getName())
            .build();
    }

    @Override
    public OrderStatus applyChangesToExistingEntity(OrderStatusPojo source, OrderStatus target) {
        throw new UnsupportedOperationException("This method is deprecated");
    }
}
