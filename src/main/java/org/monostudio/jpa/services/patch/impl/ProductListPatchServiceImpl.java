package org.monostudio.jpa.services.patch.impl;

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.ProductListPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.ProductList;
import org.monostudio.jpa.services.patch.ProductListsPatchService;

import java.util.Map;

@Service
@NoArgsConstructor
public class ProductListPatchServiceImpl
    implements ProductListsPatchService {

    @Override
    public ProductList patchExistingEntity(Map<String, Object> changes, ProductList existing) throws BadInputException {
        ProductList target = new ProductList(existing);

        if (changes.containsKey("name")) {
            String name = (String) changes.get("name");
            if (!StringUtils.isBlank(name)) {
                target.setName(name);
            }
        }

        if (changes.containsKey("code")) {
            String code = (String) changes.get("code");
            if (!StringUtils.isBlank(code)) {
                target.setCode(code);
            }
        }

        return target;
    }

    @Override
    public ProductList patchExistingEntity(ProductListPojo changes, ProductList existing) throws BadInputException {
        throw new UnsupportedOperationException("This method signature has been deprecated");
    }
}
