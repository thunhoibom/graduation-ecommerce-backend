package org.monostudio.jpa.services.crud.impl;

import com.querydsl.core.types.Predicate;
import org.apache.commons.lang3.StringUtils;
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
    private final ImagesCrudService imagesCrudService;
    private final Logger logger = LoggerFactory.getLogger(ProductsCrudServiceImpl.class);

    @Autowired
    public ProductsCrudServiceImpl(
        ProductsRepository productsRepository,
        ProductsConverterService productsConverterService,
        ProductsPatchService productsPatchService,
        ProductImagesRepository productImagesRepository,
        ImagesCrudService imagesCrudService
    ) {
        super(productsRepository, productsConverterService, productsPatchService);
        this.productsRepository = productsRepository;
        this.productsConverterService = productsConverterService;
        this.imagesCrudService = imagesCrudService;
        this.productImagesRepository = productImagesRepository;
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
        }
        return target;
    }

    @Override
    public Optional<ProductPojo> update(ProductPojo input, Long id) throws EntityNotFoundException, BadInputException {
        Product prepared = productsConverterService.convertToNewEntity(input);
        prepared.setId(id);
        Product persistent = productsRepository.saveAndFlush(prepared);
        ProductPojo target = productsConverterService.convertToPojo(persistent);
        productImagesRepository.deleteByProductId(id);
        Collection<ImagePojo> inputPojoImages = input.getImages();
        if (inputPojoImages!=null && !inputPojoImages.isEmpty()) {
            List<ProductImage> preparedImages = this.makeProductImageRelationships(persistent, inputPojoImages);
            List<ProductImage> persistentProductImages = productImagesRepository.saveAll(preparedImages);
            Collection<ImagePojo> targetPojoImages = productsConverterService.convertImagesToPojo(persistentProductImages);
            target.setImages(targetPojoImages);
        }
        return Optional.of(target);
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
        List<ProductImage> productImages = productImagesRepository.deepFindProductImagesByProductId(found.getId());
        Collection<ImagePojo> imagePojos = productsConverterService.convertImagesToPojo(productImages);
        target.setImages(imagePojos);
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
        for (ImagePojo img : inputImages) {
            try {
                Optional<Image> match = imagesCrudService.getExisting(img);
                Image image = match.orElseGet(() -> Image.builder()
                    .code(img.getCode())
                    .filename(img.getFilename())
                    .url(img.getUrl())
                    .build());
                ProductImage relationship = ProductImage.builder()
                    .product(existingProduct)
                    .image(image)
                    .build();
                allRelationships.add(relationship);
            } catch (BadInputException ex) {
                logger.debug("An image was not linked to product with barcode '{}'", existingProduct.getBarcode());
            }
        }
        return allRelationships;
    }
}
