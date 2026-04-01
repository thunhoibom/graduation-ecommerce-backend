package org.monostudio.jpa.services.conversion.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.ImagePojo;
import org.monostudio.api.models.ProductCategoryPojo;
import org.monostudio.api.models.ProductPojo;
import org.monostudio.jpa.entities.Product;
import org.monostudio.jpa.entities.ProductCategory;
import org.monostudio.jpa.entities.ProductImage;
import org.monostudio.jpa.repositories.ProductImagesRepository;
import org.monostudio.jpa.repositories.ProductsCategoriesRepository;
import org.monostudio.jpa.services.conversion.ImagesConverterService;
import org.monostudio.jpa.services.conversion.ProductCategoriesConverterService;
import org.monostudio.jpa.services.conversion.ProductsConverterService;

import java.util.Collection;
import java.util.stream.Collectors;

@Transactional
@Service
public class ProductsConverterServiceImpl
    implements ProductsConverterService {
    private final ProductImagesRepository productImagesRepository;
    private final ImagesConverterService imagesConverterService;
    private final ProductsCategoriesRepository productsCategoriesRepository;
    private final ProductCategoriesConverterService productCategoriesConverterService;

    @Autowired
    public ProductsConverterServiceImpl(
        ProductImagesRepository productImagesRepository,
        ImagesConverterService imagesConverterService,
        ProductsCategoriesRepository productsCategoriesRepository,
        ProductCategoriesConverterService productCategoriesConverterService
    ) {
        this.productImagesRepository = productImagesRepository;
        this.imagesConverterService = imagesConverterService;
        this.productsCategoriesRepository = productsCategoriesRepository;
        this.productCategoriesConverterService = productCategoriesConverterService;
    }

    @Override
    public ProductPojo convertToPojo(Product source) {
        ProductPojo target = ProductPojo.builder()
            .name(source.getName())
            .barcode(source.getBarcode())
            .price(source.getPrice())
            .description(source.getDescription())
            .currentStock(source.getStockCurrent())
            .criticalStock(source.getStockCritical())
            .build();

        ProductCategory category = source.getProductCategory();
        if (category!=null) {
            ProductCategoryPojo categoryPojo = productCategoriesConverterService.convertToPojo(category);
            target.setCategory(categoryPojo);
        }
        return target;
    }

    @Override
    public Product convertToNewEntity(ProductPojo source) {
        Product target = Product.builder()
            .name(source.getName())
            .barcode(source.getBarcode())
            .price(source.getPrice())
            .description(source.getDescription())
            .build();

        if (source.getCurrentStock()!=null) {
            target.setStockCurrent(source.getCurrentStock());
        }

        if (source.getCriticalStock()!=null) {
            target.setStockCritical(source.getCriticalStock());
        }

        ProductCategoryPojo sourceCategory = source.getCategory();
        if (sourceCategory!=null && !StringUtils.isBlank(sourceCategory.getCode())) {
            productsCategoriesRepository.findByCode(sourceCategory.getCode()).ifPresent(target::setProductCategory);
        }

        return target;
    }

    @Override
    public Collection<ImagePojo> convertImagesToPojo(Collection<ProductImage> productImages) {
        return productImages.stream()
            .map(ProductImage::getImage)
            .map(imagesConverterService::convertToPojo)
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public Product applyChangesToExistingEntity(ProductPojo source, Product target) {
        throw new UnsupportedOperationException("This method is deprecated");
    }

}
