package org.monostudio.jpa.services.conversion;

import org.monostudio.api.models.ImagePojo;
import org.monostudio.api.models.ProductPojo;
import org.monostudio.jpa.entities.Product;
import org.monostudio.jpa.entities.ProductImage;
import org.monostudio.jpa.services.ConverterService;

import java.util.Collection;

public interface ProductsConverterService
    extends ConverterService<ProductPojo, Product> {
    Collection<ImagePojo> convertImagesToPojo(Collection<ProductImage> productImages);
}
