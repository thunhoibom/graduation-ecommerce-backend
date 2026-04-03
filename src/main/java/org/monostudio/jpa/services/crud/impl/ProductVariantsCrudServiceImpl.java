package org.monostudio.jpa.services.crud.impl;

import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.ProductVariantPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.ProductVariant;
import org.monostudio.jpa.repositories.ProductVariantsRepository;
import org.monostudio.jpa.services.conversion.ProductVariantsConverterService;
import org.monostudio.jpa.services.crud.CrudGenericService;
import org.monostudio.jpa.services.crud.ProductVariantsCrudService;
import org.monostudio.jpa.services.patch.ProductVariantsPatchService;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;

@Transactional
@Service
public class ProductVariantsCrudServiceImpl
    extends CrudGenericService<ProductVariantPojo, ProductVariant>
    implements ProductVariantsCrudService {

    private final ProductVariantsRepository productVariantsRepository;
    private final ProductVariantsConverterService productVariantsConverterService;

    @Autowired
    public ProductVariantsCrudServiceImpl(
        ProductVariantsRepository productVariantsRepository,
        ProductVariantsConverterService productVariantsConverterService,
        ProductVariantsPatchService productVariantsPatchService
    ) {
        super(productVariantsRepository, productVariantsConverterService, productVariantsPatchService);
        this.productVariantsRepository = productVariantsRepository;
        this.productVariantsConverterService = productVariantsConverterService;
    }

    @Override
    @Transactional
    public ProductVariantPojo create(ProductVariantPojo input) throws BadInputException, EntityExistsException {
        this.validateInputPojoBeforeCreation(input);
        ProductVariant prepared = productVariantsConverterService.convertToNewEntity(input);
        ProductVariant persistent = productVariantsRepository.saveAndFlush(prepared);
        return productVariantsConverterService.convertToPojo(persistent);
    }

    @Override
    @Transactional
    public Optional<ProductVariantPojo> update(ProductVariantPojo input, Long id)
        throws EntityNotFoundException, BadInputException {
        ProductVariant prepared = productVariantsConverterService.convertToNewEntity(input);
        prepared.setId(id);
        ProductVariant persistent = productVariantsRepository.saveAndFlush(prepared);
        return Optional.of(productVariantsConverterService.convertToPojo(persistent));
    }

    @Override
    @Transactional
    public ProductVariantPojo readOne(Predicate filters) throws EntityNotFoundException {
        Optional<ProductVariant> entity = productVariantsRepository.findOne(filters);
        if (entity.isEmpty()) {
            throw new EntityNotFoundException(ITEM_NOT_FOUND);
        }
        return productVariantsConverterService.convertToPojo(entity.get());
    }

    @Override
    public Optional<ProductVariant> getExisting(ProductVariantPojo input) throws BadInputException {
        String sku = input.getSku();
        if (sku == null || sku.isBlank()) {
            throw new BadInputException("Invalid variant SKU");
        }
        return productVariantsRepository.findBySku(sku);
    }
}
