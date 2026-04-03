package org.monostudio.jpa.services.conversion;

import org.monostudio.api.models.ProductVariantPojo;
import org.monostudio.jpa.entities.ProductVariant;
import org.monostudio.jpa.services.ConverterService;

public interface ProductVariantsConverterService
    extends ConverterService<ProductVariantPojo, ProductVariant> {
}
