package org.monostudio.jpa.services.crud.impl;

import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.AddressPojo;
import org.monostudio.api.models.OrderDetailPojo;
import org.monostudio.api.models.OrderPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.config.ApiProperties;
import org.monostudio.jpa.entities.Order;
import org.monostudio.jpa.repositories.OrdersRepository;
import org.monostudio.jpa.services.conversion.AddressesConverterService;
import org.monostudio.jpa.services.conversion.OrdersConverterService;
import org.monostudio.jpa.services.crud.CrudGenericService;
import org.monostudio.jpa.services.crud.OrdersCrudService;
import org.monostudio.jpa.services.patch.OrdersPatchService;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Transactional
@Service
public class OrdersCrudServiceImpl
    extends CrudGenericService<OrderPojo, Order>
    implements OrdersCrudService {
    private final OrdersRepository ordersRepository;
    private final OrdersConverterService ordersConverterService;
    private final AddressesConverterService addressesConverterService;
    private final ApiProperties apiProperties;

    @Autowired
    public OrdersCrudServiceImpl(
        OrdersRepository ordersRepository,
        OrdersConverterService ordersConverterService,
        OrdersPatchService ordersPatchService,
        AddressesConverterService addressesConverterService,
        ApiProperties apiProperties
    ) {
        super(ordersRepository, ordersConverterService, ordersPatchService);
        this.ordersRepository = ordersRepository;
        this.ordersConverterService = ordersConverterService;
        this.addressesConverterService = addressesConverterService;
        this.apiProperties = apiProperties;
    }

    @Override
    public Optional<Order> getExisting(OrderPojo input) {
        Long buyOrder = input.getBuyOrder();
        if (buyOrder==null) {
            return Optional.empty();
        } else {
            return this.ordersRepository.findById(buyOrder);
        }
    }

    @Override
    public OrderPojo readOne(Predicate conditions) throws EntityNotFoundException {
        Optional<Order> matchingSell = ordersRepository.findOne(conditions);
        if (matchingSell.isPresent()) {
            Order found = matchingSell.get();
            OrderPojo target = ordersConverterService.convertToPojo(found);

            AddressPojo billingAddress = addressesConverterService.convertToPojo(found.getBillingAddress());
            target.setBillingAddress(billingAddress);

            if (found.getShippingAddress()!=null) {
                AddressPojo shippingAddress = addressesConverterService.convertToPojo(found.getShippingAddress());
                target.setShippingAddress(shippingAddress);
            }

            List<OrderDetailPojo> details = found.getDetails().stream()
                .map(ordersConverterService::convertDetailToPojo)
                .collect(Collectors.toList());
            target.setDetails(details);

            return target;
        } else {
            throw new EntityNotFoundException("No sell matches the filtering conditions");
        }
    }

    @Override
    protected Order flushPartialChanges(Map<String, Object> changes, Order existingEntity) throws BadInputException {
        Integer statusCode = existingEntity.getStatus().getCode();
        if ((statusCode >= 3 || statusCode < 0) && !apiProperties.isAbleToEditOrdersAfterBeingProcessed()) {
            throw new BadInputException("The requested transaction cannot be modified");
        }
        return super.flushPartialChanges(changes, existingEntity);
    }
}
