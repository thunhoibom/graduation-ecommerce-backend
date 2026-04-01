package org.monostudio.jpa.services.conversion;

import org.monostudio.api.models.ProductListPojo;
import org.monostudio.jpa.entities.ProductList;
import org.monostudio.jpa.services.ConverterService;

public interface ProductListsConverterService
    extends ConverterService<ProductListPojo, ProductList> {
}
