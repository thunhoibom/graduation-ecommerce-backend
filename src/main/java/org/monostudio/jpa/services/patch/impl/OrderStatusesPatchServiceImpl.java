package org.monostudio.jpa.services.patch.impl;

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.OrderStatusPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.OrderStatus;
import org.monostudio.jpa.services.patch.OrderStatusesPatchService;

import java.util.Map;

@Service
@NoArgsConstructor
public class OrderStatusesPatchServiceImpl
    implements OrderStatusesPatchService {

    @Override
    public OrderStatus patchExistingEntity(Map<String, Object> changes, OrderStatus existing) throws BadInputException {
        OrderStatus target = new OrderStatus(existing);

        if (changes.containsKey("code")) {
            Integer code = (Integer) changes.get("code");
            if (code!=null) {
                target.setCode(code);
            }
        }

        if (changes.containsKey("name")) {
            String name = (String) changes.get("name");
            if (!StringUtils.isBlank(name)) {
                target.setName(name);
            }
        }

        return target;
    }

    @Override
    public OrderStatus patchExistingEntity(OrderStatusPojo changes, OrderStatus existing) throws BadInputException {
        throw new UnsupportedOperationException("This method has been deprecated");
    }
}
