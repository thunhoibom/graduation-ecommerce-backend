package org.monostudio.jpa.services.patch.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.ProductVariantPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.ProductVariant;
import org.monostudio.jpa.repositories.ProductsRepository;
import org.monostudio.jpa.services.patch.ProductVariantsPatchService;

import java.util.Map;

@Service
public class ProductVariantsPatchServiceImpl
    implements ProductVariantsPatchService {

    private final ProductsRepository productsRepository;

    @Autowired
    public ProductVariantsPatchServiceImpl(ProductsRepository productsRepository) {
        this.productsRepository = productsRepository;
    }

    @Override
    public ProductVariant patchExistingEntity(Map<String, Object> changes, ProductVariant existing)
        throws BadInputException {
        ProductVariant target = new ProductVariant(existing);

        if (changes.containsKey("sku")) {
            String sku = (String) changes.get("sku");
            if (!StringUtils.isBlank(sku)) {
                target.setSku(sku);
            }
        }

        if (changes.containsKey("size")) {
            String size = (String) changes.get("size");
            if (!StringUtils.isBlank(size)) {
                target.setSize(size);
            }
        }

        if (changes.containsKey("color")) {
            String color = (String) changes.get("color");
            if (!StringUtils.isBlank(color)) {
                target.setColor(color);
            }
        }

        if (changes.containsKey("attributes")) {
            String attributes = (String) changes.get("attributes");
            if (!StringUtils.isBlank(attributes)) {
                target.setAttributes(attributes);
            }
        }

        if (changes.containsKey("priceModifier")) {
            Integer priceModifier = (Integer) changes.get("priceModifier");
            if (priceModifier != null) {
                target.setPriceModifier(priceModifier);
            }
        }

        if (changes.containsKey("currentStock")) {
            Integer currentStock = (Integer) changes.get("currentStock");
            if (currentStock != null) {
                target.setStockCurrent(currentStock);
            }
        }

        if (changes.containsKey("criticalStock")) {
            Integer criticalStock = (Integer) changes.get("criticalStock");
            if (criticalStock != null) {
                target.setStockCritical(criticalStock);
            }
        }

        // stockReserved is managed exclusively by the Stock Reservation system —
        // it should NOT be directly patchable via admin endpoints.
        // Omit patch handler here to enforce that constraint.

        if (changes.containsKey("active")) {
            Boolean active = (Boolean) changes.get("active");
            if (active != null) {
                target.setActive(active);
            }
        }

        if (changes.containsKey("barcode")) {
            String barcode = (String) changes.get("barcode");
            if (!StringUtils.isBlank(barcode)) {
                target.setBarcode(barcode);
            }
        }

        if (changes.containsKey("productBarcode")) {
            String productBarcode = (String) changes.get("productBarcode");
            if (!StringUtils.isBlank(productBarcode)) {
                productsRepository.findByBarcode(productBarcode)
                    .ifPresent(target::setProduct);
            }
        }

        return target;
    }

    @Override
    public ProductVariant patchExistingEntity(ProductVariantPojo changes, ProductVariant existing)
        throws BadInputException {
        throw new UnsupportedOperationException("This method signature has been deprecated");
    }
}
