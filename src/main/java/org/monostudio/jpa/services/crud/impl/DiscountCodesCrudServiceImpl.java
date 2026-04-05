package org.monostudio.jpa.services.crud.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.DiscountCodePojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.DiscountCode;
import org.monostudio.jpa.repositories.DiscountCodesRepository;
import org.monostudio.jpa.services.conversion.DiscountCodesConverterService;
import org.monostudio.jpa.services.crud.CrudGenericService;
import org.monostudio.jpa.services.crud.DiscountCodesCrudService;
import org.monostudio.jpa.services.patch.DiscountCodesPatchService;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;

@Transactional
@Service
public class DiscountCodesCrudServiceImpl
    extends CrudGenericService<DiscountCodePojo, DiscountCode>
    implements DiscountCodesCrudService {

    private final DiscountCodesRepository discountCodesRepository;

    @Autowired
    public DiscountCodesCrudServiceImpl(
        DiscountCodesRepository discountCodesRepository,
        DiscountCodesConverterService discountCodesConverterService,
        DiscountCodesPatchService discountCodesPatchService
    ) {
        super(discountCodesRepository, discountCodesConverterService, discountCodesPatchService);
        this.discountCodesRepository = discountCodesRepository;
    }

    @Override
    @Transactional
    public DiscountCodePojo create(DiscountCodePojo input) throws BadInputException, EntityExistsException {
        this.validateInputPojoBeforeCreation(input);
        DiscountCode prepared = converter.convertToNewEntity(input);
        DiscountCode persistent = discountCodesRepository.saveAndFlush(prepared);
        return converter.convertToPojo(persistent);
    }

    @Override
    @Transactional
    public Optional<DiscountCodePojo> update(DiscountCodePojo input, Long id)
        throws EntityNotFoundException, BadInputException {
        DiscountCode prepared = converter.convertToNewEntity(input);
        prepared.setId(id);
        DiscountCode persistent = discountCodesRepository.saveAndFlush(prepared);
        return Optional.of(converter.convertToPojo(persistent));
    }

    @Override
    public Optional<DiscountCode> getExisting(DiscountCodePojo input) throws BadInputException {
        String code = input.getCode();
        if (code == null || code.isBlank()) {
            throw new BadInputException("Invalid discount code");
        }
        return discountCodesRepository.findByCodeIgnoreCase(code);
    }
}
