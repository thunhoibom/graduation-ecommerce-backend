package org.monostudio.jpa.services.crud.impl;

import com.querydsl.core.types.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.monostudio.jpa.repositories.ProductsCategoriesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.ImagePojo;
import org.monostudio.api.models.ProductPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Image;
import org.monostudio.jpa.entities.Product;
import org.monostudio.jpa.entities.ProductImage;
import org.monostudio.jpa.repositories.ProductImagesRepository;
import org.monostudio.jpa.repositories.ProductsRepository;
import org.monostudio.jpa.services.conversion.ProductsConverterService;
import org.monostudio.jpa.services.crud.CrudGenericService;
import org.monostudio.jpa.services.crud.ImagesCrudService;
import org.monostudio.jpa.services.crud.ProductsCrudService;
import org.monostudio.jpa.services.patch.ProductsPatchService;
import org.monostudio.search.kafka.IndexEventProducer;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Transactional
@Service
public class ProductsCrudServiceImpl
    extends CrudGenericService<ProductPojo, Product>
    implements ProductsCrudService {
    private final ProductsRepository productsRepository;
    private final ProductsConverterService productsConverterService;
    private final ProductImagesRepository productImagesRepository;
    private final ProductsCategoriesRepository productsCategoriesRepository;
    private final ImagesCrudService imagesCrudService;
    private final IndexEventProducer indexEventProducer;
    private final Logger logger = LoggerFactory.getLogger(ProductsCrudServiceImpl.class);

    @Autowired
    public ProductsCrudServiceImpl(
        ProductsRepository productsRepository,
        ProductsConverterService productsConverterService,
        ProductsPatchService productsPatchService,
        ProductImagesRepository productImagesRepository,
        ProductsCategoriesRepository productsCategoriesRepository,
        ImagesCrudService imagesCrudService,
        IndexEventProducer indexEventProducer
    ) {
        super(productsRepository, productsConverterService, productsPatchService);
        this.productsRepository = productsRepository;
        this.productsConverterService = productsConverterService;
        this.imagesCrudService = imagesCrudService;
        this.productImagesRepository = productImagesRepository;
        this.productsCategoriesRepository = productsCategoriesRepository;
        this.indexEventProducer = indexEventProducer;
    }


    @Transactional
    @Override
    public ProductPojo create(ProductPojo input)
        throws BadInputException, EntityExistsException {
        this.validateInputPojoBeforeCreation(input);
        Product prepared = productsConverterService.convertToNewEntity(input);
        Product persistent = productsRepository.saveAndFlush(prepared);
        ProductPojo target = productsConverterService.convertToPojo(persistent);
        Collection<ImagePojo> inputPojoImages = input.getImages();
        if (inputPojoImages!=null && !inputPojoImages.isEmpty()) {
            List<ProductImage> preparedImages = this.makeProductImageRelationships(persistent, inputPojoImages);
            List<ProductImage> persistentProductImages = productImagesRepository.saveAll(preparedImages);
            Collection<ImagePojo> targetPojoImages = productsConverterService.convertImagesToPojo(persistentProductImages);
            target.setImages(targetPojoImages);
            target.setPrimaryImageUrl(productsConverterService.extractPrimaryImageUrl(persistentProductImages));
        }

        indexEventProducer.sendIndexEvent("PRODUCT", persistent.getId(), "CREATE");
        return target;
    }

    @Override
    public Optional<ProductPojo> update(ProductPojo input, Long id) throws EntityNotFoundException, BadInputException {
        Optional<ProductPojo> pojo = this.fullUpdateProduct(input, id);
        pojo.ifPresent(p -> indexEventProducer.sendIndexEvent("PRODUCT", p.getId(), "UPDATE"));
        return pojo;
    }

    private Optional<ProductPojo> fullUpdateProduct(ProductPojo input, Long id) throws EntityNotFoundException, BadInputException {
        Product existing = productsRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException(ITEM_NOT_FOUND));

        // Update fields manually to preserve collections (like variants) and other unmapped data
        existing.setName(input.getName());
        existing.setBarcode(input.getBarcode());
        existing.setDescription(input.getDescription());
        existing.setPrice(input.getPrice());

        if (input.getCurrentStock() != null) {
            existing.setStockCurrent(input.getCurrentStock());
        }
        if (input.getCriticalStock() != null) {
            existing.setStockCritical(input.getCriticalStock());
        }

        if (input.getCategory() != null && StringUtils.isNotBlank(input.getCategory().getCode())) {
            productsCategoriesRepository.findByCode(input.getCategory().getCode())
                .ifPresent(existing::setProductCategory);
        }

        if (StringUtils.isNotBlank(input.getStatus())) {
            try {
                existing.setStatus(org.monostudio.jpa.entities.ProductStatus.valueOf(input.getStatus()));
            } catch (IllegalArgumentException e) {
                // Keep existing status if invalid
            }
        }

        Product persistent = productsRepository.saveAndFlush(existing);
        ProductPojo target = productsConverterService.convertToPojo(persistent);

        productImagesRepository.deleteByProductId(id);
        Collection<ImagePojo> inputPojoImages = input.getImages();
        if (inputPojoImages != null && !inputPojoImages.isEmpty()) {
            List<ProductImage> preparedImages = this.makeProductImageRelationships(persistent, inputPojoImages);
            List<ProductImage> persistentProductImages = productImagesRepository.saveAll(preparedImages);
            Collection<ImagePojo> targetPojoImages = productsConverterService.convertImagesToPojo(persistentProductImages);
            target.setImages(targetPojoImages);
            target.setPrimaryImageUrl(productsConverterService.extractPrimaryImageUrl(persistentProductImages));
        }

        return Optional.of(target);
    }

    @Override
    public Optional<ProductPojo> partialUpdate(java.util.Map<String, Object> changes, Long id) {
        Optional<ProductPojo> pojo = super.partialUpdate(changes, id);
        pojo.ifPresent(p -> indexEventProducer.sendIndexEvent("PRODUCT", p.getId(), "UPDATE"));
        return pojo;
    }

    @Override
    public Optional<ProductPojo> partialUpdate(java.util.Map<String, Object> changes, com.querydsl.core.types.Predicate filters) {
        Optional<ProductPojo> pojo = super.partialUpdate(changes, filters);
        pojo.ifPresent(p -> indexEventProducer.sendIndexEvent("PRODUCT", p.getId(), "UPDATE"));
        return pojo;
    }


    @Override
    public void delete(Long id) throws EntityNotFoundException {
        super.delete(id);
        indexEventProducer.sendIndexEvent("PRODUCT", id, "DELETE");
    }

    @Override
    public ProductPojo readOne(Predicate filters)
        throws EntityNotFoundException {
        Optional<Product> entity = productsRepository.findOne(filters);
        if (entity.isEmpty()) {
            throw new EntityNotFoundException(ITEM_NOT_FOUND);
        }
        Product found = entity.get();
        ProductPojo target = productsConverterService.convertToPojo(found);
        List<ProductImage> productImages = productImagesRepository.deepFindProductImagesByProductIdOrdered(found.getId());
        Collection<ImagePojo> imagePojos = productsConverterService.convertImagesToPojo(productImages);
        target.setImages(imagePojos);
        target.setPrimaryImageUrl(productsConverterService.extractPrimaryImageUrl(productImages));
        return target;
    }

    @Override
    public Optional<Product> getExisting(ProductPojo input)
        throws BadInputException {
        String barcode = input.getBarcode();
        if (StringUtils.isBlank(barcode)) {
            throw new BadInputException("Invalid product barcode");
        } else {
            return productsRepository.findByBarcode(barcode);
        }
    }

    /**
     * Creates transient instances of the ProductImages entity. Does NOT persist said instances.
     *
     * @param existingProduct The persisted entity
     * @param inputImages     The list of images to link to the aforementioned Product
     * @return The list of ImagePojos with normalized metadata.
     */
    private List<ProductImage> makeProductImageRelationships(Product existingProduct, Collection<ImagePojo> inputImages) {
        List<ProductImage> allRelationships = new ArrayList<>();
        if (inputImages == null || inputImages.isEmpty()) {
            return allRelationships;
        }

        // Only process the first image as requested (limit 1)
        ImagePojo img = inputImages.iterator().next();
        try {
            Optional<Image> match = imagesCrudService.getExisting(img);
            if (match.isPresent()) {
                ProductImage relationship = ProductImage.builder()
                    .product(existingProduct)
                    .image(match.get())
                    .sortOrder(0)
                    .isPrimary(true)
                    .build();
                allRelationships.add(relationship);
            } else {
                logger.warn("Image with code/filename '{}' not found in database. Skipping linkage to product '{}'",
                    img.getCode() != null ? img.getCode() : img.getFilename(), existingProduct.getBarcode());
            }
        } catch (BadInputException ex) {
            logger.debug("An image was not linked to product with barcode '{}'", existingProduct.getBarcode());
        }
        
        return allRelationships;
    }


}
