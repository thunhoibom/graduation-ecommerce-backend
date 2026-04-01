package org.monostudio.jpa.services.crud.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.ShipperPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Shipper;
import org.monostudio.jpa.repositories.ShippersRepository;
import org.monostudio.jpa.services.conversion.ShippersConverterService;
import org.monostudio.jpa.services.crud.CrudGenericService;
import org.monostudio.jpa.services.crud.ShippersCrudService;
import org.monostudio.jpa.services.patch.ShippersPatchService;

import java.util.Optional;

@Transactional
@Service
public class ShippersCrudServiceImpl
    extends CrudGenericService<ShipperPojo, Shipper>
    implements ShippersCrudService {
    private final ShippersRepository shippersRepository;

    @Autowired
    public ShippersCrudServiceImpl(
        ShippersRepository shippersRepository,
        ShippersConverterService shippersConverterService,
        ShippersPatchService shippersPatchService
    ) {
        super(shippersRepository, shippersConverterService, shippersPatchService);
        this.shippersRepository = shippersRepository;
    }

    @Override
    public Optional<Shipper> getExisting(ShipperPojo input) throws BadInputException {
        String name = input.getName();
        if (StringUtils.isBlank(name)) {
            throw new BadInputException("Billing type has no name");
        } else {
            return shippersRepository.findByName(name);
        }
    }
}
