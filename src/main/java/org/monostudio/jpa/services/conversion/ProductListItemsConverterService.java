package org.monostudio.jpa.services.conversion;

import org.monostudio.api.models.ProductPojo;
import org.monostudio.jpa.entities.ProductListItem;
import org.monostudio.jpa.services.ConverterService;

public interface ProductListItemsConverterService
    extends ConverterService<ProductPojo, ProductListItem> {
}
