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

import jakarta.persistence.EntityNotFoundException;
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

    /**
     * Full PUT may omit fields (e.g. admin toggles {@code active} with a partial payload). Merge with the row in DB instead of blind {@code convertToNewEntity}.
     */
    @Override
    public Optional<ShippingMethodPojo> update(ShippingMethodPojo input, Long id)
        throws EntityNotFoundException, BadInputException {
        ShippingMethod existing = shippingMethodsRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("No shipping method with id " + id));
        ShippingMethodsConverterService smConverter = (ShippingMethodsConverterService) converter;
        ShippingMethod merged = smConverter.mergePojoOntoExisting(input, existing);
        ShippingMethod saved = shippingMethodsRepository.saveAndFlush(merged);
        return Optional.of(converter.convertToPojo(saved));
    }

    @Override
    public Optional<ShippingMethod> getExisting(ShippingMethodPojo input) throws BadInputException {
        if (StringUtils.isBlank(input.getName())) {
            return Optional.empty();
        }
        return shippingMethodsRepository.findByName(input.getName());
    }
}
