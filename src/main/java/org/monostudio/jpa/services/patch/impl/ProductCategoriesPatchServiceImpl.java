package org.monostudio.jpa.services.patch.impl;

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.ProductCategoryPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.ProductCategory;
import org.monostudio.jpa.services.patch.ProductCategoriesPatchService;

import java.util.Map;

@Service
@NoArgsConstructor
public class ProductCategoriesPatchServiceImpl
    implements ProductCategoriesPatchService {

    @Override
    public ProductCategory patchExistingEntity(Map<String, Object> changes, ProductCategory existing) throws BadInputException {
        ProductCategory target = new ProductCategory(existing);

        if (changes.containsKey("code")) {
            String code = (String) changes.get("code");
            if (!StringUtils.isBlank(code)) {
                target.setCode(code);
            }
        }

        if (changes.containsKey("name")) {
            String name = (String) changes.get("name");
            if (!StringUtils.isBlank(name)) {
                target.setName(name);
            }
        }

        return target;
    }

    @Override
    public ProductCategory patchExistingEntity(ProductCategoryPojo changes, ProductCategory existing) throws BadInputException {
        throw new UnsupportedOperationException("This method signature has been deprecated");
    }
}
