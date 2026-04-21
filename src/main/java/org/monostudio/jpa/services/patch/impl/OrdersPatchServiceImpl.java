package org.monostudio.jpa.services.patch.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.OrderPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Order;
import org.monostudio.jpa.repositories.PaymentTypesRepository;
import org.monostudio.jpa.repositories.ShippingMethodsRepository;
import org.monostudio.jpa.services.patch.OrdersPatchService;

import java.time.Instant;
import java.util.Map;

@Transactional
@Service
public class OrdersPatchServiceImpl
    implements OrdersPatchService {
    private final PaymentTypesRepository paymentTypesRepository;
    private final ShippingMethodsRepository shippingMethodsRepository;

    @Autowired
    public OrdersPatchServiceImpl(
        PaymentTypesRepository paymentTypesRepository,
        ShippingMethodsRepository shippingMethodsRepository
    ) {
        this.paymentTypesRepository = paymentTypesRepository;
        this.shippingMethodsRepository = shippingMethodsRepository;
    }

    @Transactional
    @Override
    public Order patchExistingEntity(Map<String, Object> changes, Order existing) throws BadInputException {
        Order target = new Order(existing);

        try {
            if (changes.containsKey("date")) {
                String rawDate = (String) changes.get("date");
                if (rawDate!=null) {
                    Instant date = Instant.parse(rawDate);
                    target.setDate(date);
                }
            }

            if (changes.containsKey("status")) {
                String statusName = (String) changes.get("status");
                if (!StringUtils.isBlank(statusName)) {
                    throw new BadInputException(
                        "Order status cannot be patched directly. "
                            + "Use process endpoints to enforce valid transitions."
                    );
                }
            }

            if (changes.containsKey("paymentType")) {
                String paymentTypeName = (String) changes.get("paymentType");
                if (!StringUtils.isBlank(paymentTypeName)) {
                    paymentTypesRepository.findByName(paymentTypeName).ifPresent(target::setPaymentType);
                }
            }

            if (changes.containsKey("shipper")) {
                String shipperName = (String) changes.get("shipper");
                if (!StringUtils.isBlank(shipperName)) {
                    shippingMethodsRepository.findByName(shipperName).ifPresent(target::setShippingMethod);
                }
            }
        } catch (ClassCastException ex) {
            throw new BadInputException("The ");
        }

        return target;
    }

    @Override
    public Order patchExistingEntity(OrderPojo changes, Order existing) throws BadInputException {
        throw new UnsupportedOperationException("This method signature has been deprecated");
    }
}
