package org.monostudio.jpa.services.crud;

import org.monostudio.api.models.ProductPojo;
import org.monostudio.jpa.entities.Product;
import org.monostudio.jpa.services.CrudService;

public interface ProductsCrudService
    extends CrudService<ProductPojo, Product> {
}
