package org.monostudio.jpa.services.patch;

import org.monostudio.api.models.ProductListPojo;
import org.monostudio.jpa.entities.ProductList;
import org.monostudio.jpa.services.PatchService;

public interface ProductListsPatchService
    extends PatchService<ProductListPojo, ProductList> {
}
