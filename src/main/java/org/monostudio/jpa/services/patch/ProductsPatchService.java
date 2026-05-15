package org.monostudio.jpa.services.patch;

import org.monostudio.api.models.ProductPojo;
import org.monostudio.jpa.entities.Product;
import org.monostudio.jpa.services.PatchService;

public interface ProductsPatchService
    extends PatchService<ProductPojo, Product> {
}
