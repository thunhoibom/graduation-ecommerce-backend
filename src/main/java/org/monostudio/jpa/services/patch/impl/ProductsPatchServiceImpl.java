package org.monostudio.jpa.services.patch.impl;

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.ProductPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Product;
import org.monostudio.jpa.entities.ProductStatus;
import org.monostudio.jpa.services.patch.ProductsPatchService;

import java.util.Map;

@Service
@NoArgsConstructor
public class ProductsPatchServiceImpl
    implements ProductsPatchService {

    @Override
    public Product patchExistingEntity(Map<String, Object> changes, Product existing) throws BadInputException {
        Product target = new Product(existing);
        target.setVariants(existing.getVariants());
        target.setReviews(existing.getReviews());
        target.setProductCategory(existing.getProductCategory());

        if (changes.containsKey("barcode")) {
            String barcode = (String) changes.get("barcode");
            if (!StringUtils.isBlank(barcode)) {
                target.setBarcode(barcode);
            }
        }

        if (changes.containsKey("name")) {
            String name = (String) changes.get("name");
            if (!StringUtils.isBlank(name)) {
                target.setName(name);
            }
        }

        if (changes.containsKey("price")) {
            Integer price = (Integer) changes.get("price");
            target.setPrice(price);
        }

        if (changes.containsKey("description")) {
            String description = (String) changes.get("description");
            if (!StringUtils.isBlank(description)) {
                target.setDescription(description);
            }
        }

        if (changes.containsKey("currentStock")) {
            Integer currentStock = (Integer) changes.get("currentStock");
            target.setStockCurrent(currentStock);
        }

        if (changes.containsKey("criticalStock")) {
            Integer criticalStock = (Integer) changes.get("criticalStock");
            target.setStockCritical(criticalStock);
        }

        if (changes.containsKey("status")) {
            Object statusRaw = changes.get("status");
            if (statusRaw != null) {
                try {
                    ProductStatus status = ProductStatus.valueOf(statusRaw.toString().toUpperCase());
                    target.setStatus(status);
                } catch (IllegalArgumentException exc) {
                    throw new BadInputException("Invalid product status: " + statusRaw);
                }
            }
        }

        return target;
    }

    @Override
    public Product patchExistingEntity(ProductPojo changes, Product existing) throws BadInputException {
        throw new UnsupportedOperationException("This method signature has been deprecated");
    }
}
