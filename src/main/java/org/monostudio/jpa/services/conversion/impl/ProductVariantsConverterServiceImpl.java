package org.monostudio.jpa.services.conversion.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.ProductVariantPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Product;
import org.monostudio.jpa.entities.ProductVariant;
import org.monostudio.jpa.repositories.ProductsRepository;
import org.monostudio.jpa.services.conversion.ProductVariantsConverterService;

@Transactional
@Service
public class ProductVariantsConverterServiceImpl
    implements ProductVariantsConverterService {

    private final ProductsRepository productsRepository;

    @Autowired
    public ProductVariantsConverterServiceImpl(ProductsRepository productsRepository) {
        this.productsRepository = productsRepository;
    }

    @Override
    public ProductVariantPojo convertToPojo(ProductVariant source) {
        ProductVariantPojo target = ProductVariantPojo.builder()
            .sku(source.getSku())
            .size(source.getSize())
            .color(source.getColor())
            .attributes(source.getAttributes())
            .priceModifier(source.getPriceModifier())
            .currentStock(source.getStockCurrent())
            .criticalStock(source.getStockCritical())
            .reservedStock(source.getStockReserved())
            .availableStock(source.getStockCurrent() - source.getStockReserved())
            .active(source.isActive())
            .barcode(source.getBarcode())
            .createdAt(source.getCreatedAt())
            .build();

        Product product = source.getProduct();
        if (product != null) {
            target.setProductBarcode(product.getBarcode());
            target.setProductName(product.getName());
            target.setProductBasePrice(product.getPrice());
            target.setFinalPrice(product.getPrice() + source.getPriceModifier());
        }

        return target;
    }

    @Override
    public ProductVariant convertToNewEntity(ProductVariantPojo source) throws BadInputException {
        ProductVariant target = ProductVariant.builder()
            .sku(source.getSku())
            .size(source.getSize())
            .color(source.getColor())
            .attributes(source.getAttributes())
            .priceModifier(source.getPriceModifier() != null ? source.getPriceModifier() : 0)
            .active(source.getActive() != null ? source.getActive() : true)
            .barcode(source.getBarcode())
            .build();

        if (source.getCurrentStock() != null) {
            target.setStockCurrent(source.getCurrentStock());
        }

        if (source.getCriticalStock() != null) {
            target.setStockCritical(source.getCriticalStock());
        }

        if (!StringUtils.isBlank(source.getProductBarcode())) {
            productsRepository.findByBarcode(source.getProductBarcode())
                .ifPresent(target::setProduct);
        } else {
            throw new BadInputException("Product barcode is required to create a variant");
        }

        return target;
    }

    @Override
    public ProductVariant applyChangesToExistingEntity(ProductVariantPojo source, ProductVariant target) {
        throw new UnsupportedOperationException("This method is deprecated");
    }
}
