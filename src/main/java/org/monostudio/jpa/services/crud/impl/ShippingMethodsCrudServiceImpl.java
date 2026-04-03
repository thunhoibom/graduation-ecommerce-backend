package org.monostudio.jpa.services.crud.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.ShippingMethodPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.ShippingMethod;
import org.monostudio.jpa.repositories.ShippingMethodsRepository;
import org.monostudio.jpa.services.conversion.ShippingMethodsConverterService;
import org.monostudio.jpa.services.crud.CrudGenericService;
import org.monostudio.jpa.services.crud.ShippingMethodsCrudService;
import org.monostudio.jpa.services.patch.ShippingMethodsPatchService;

import java.util.Optional;

@Transactional
@Service
public class ShippingMethodsCrudServiceImpl
    extends CrudGenericService<ShippingMethodPojo, ShippingMethod>
    implements ShippingMethodsCrudService {
    private final ShippingMethodsRepository shippingMethodsRepository;

    @Autowired
    public ShippingMethodsCrudServiceImpl(
        ShippingMethodsRepository shippingMethodsRepository,
        ShippingMethodsConverterService shippingMethodsConverterService,
        ShippingMethodsPatchService shippingMethodsPatchService
    ) {
        super(shippingMethodsRepository, shippingMethodsConverterService, shippingMethodsPatchService);
        this.shippingMethodsRepository = shippingMethodsRepository;
    }

    @Override
    public Optional<ShippingMethod> getExisting(ShippingMethodPojo input) throws BadInputException {
        String name = input.getName();
        if (StringUtils.isBlank(name)) {
            throw new BadInputException("Shipping method has no name");
        } else {
            return shippingMethodsRepository.findByName(name);
        }
    }
}
