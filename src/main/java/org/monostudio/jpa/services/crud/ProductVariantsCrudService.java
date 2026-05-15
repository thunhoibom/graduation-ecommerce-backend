package org.monostudio.jpa.services.crud;

import org.monostudio.api.models.ProductVariantPojo;
import org.monostudio.jpa.entities.ProductVariant;
import org.monostudio.jpa.services.CrudService;

public interface ProductVariantsCrudService
    extends CrudService<ProductVariantPojo, ProductVariant> {
}
