package org.monostudio.jpa.services.crud.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.OrderStatusPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.OrderStatus;
import org.monostudio.jpa.repositories.OrderStatusesRepository;
import org.monostudio.jpa.services.conversion.OrderStatusesConverterService;
import org.monostudio.jpa.services.crud.CrudGenericService;
import org.monostudio.jpa.services.crud.OrderStatusesCrudService;
import org.monostudio.jpa.services.patch.OrderStatusesPatchService;

import java.util.Optional;

@Transactional
@Service
public class OrderStatusesCrudServiceImpl
    extends CrudGenericService<OrderStatusPojo, OrderStatus>
    implements OrderStatusesCrudService {
    private final OrderStatusesRepository statusesRepository;

    @Autowired
    public OrderStatusesCrudServiceImpl(
        OrderStatusesRepository statusesRepository,
        OrderStatusesConverterService statusesConverterService,
        OrderStatusesPatchService statusesPatchService
    ) {
        super(statusesRepository, statusesConverterService, statusesPatchService);
        this.statusesRepository = statusesRepository;
    }

    @Override
    public Optional<OrderStatus> getExisting(OrderStatusPojo input) throws BadInputException {
        String name = input.getName();
        if (StringUtils.isBlank(name)) {
            throw new BadInputException("Invalid status name");
        } else {
            return statusesRepository.findByName(name);
        }
    }
}
