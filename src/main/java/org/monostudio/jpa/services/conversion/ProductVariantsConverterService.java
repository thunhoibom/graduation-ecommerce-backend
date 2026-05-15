package org.monostudio.jpa.services.conversion;

import org.monostudio.api.models.ImagePojo;
import org.monostudio.api.models.ProductVariantPojo;
import org.monostudio.jpa.entities.ProductVariant;
import org.monostudio.jpa.entities.VariantImage;
import org.monostudio.jpa.services.ConverterService;

import java.util.Collection;

public interface ProductVariantsConverterService
    extends ConverterService<ProductVariantPojo, ProductVariant> {

    /**
     * Converts a collection of VariantImages to ImagePojos, sorted by sortOrder.
     */
    Collection<ImagePojo> convertVariantImagesToPojo(Collection<VariantImage> variantImages);

    /**
     * Extracts the primary image URL from variant images.
     * Returns null if no primary image is set.
     */
    String extractPrimaryImageUrl(Collection<VariantImage> variantImages);
}
