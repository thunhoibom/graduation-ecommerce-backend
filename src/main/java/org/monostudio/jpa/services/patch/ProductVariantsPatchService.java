package org.monostudio.jpa.services.patch;

import org.monostudio.api.models.ProductVariantPojo;
import org.monostudio.jpa.entities.ProductVariant;
import org.monostudio.jpa.services.PatchService;

public interface ProductVariantsPatchService
    extends PatchService<ProductVariantPojo, ProductVariant> {
}
