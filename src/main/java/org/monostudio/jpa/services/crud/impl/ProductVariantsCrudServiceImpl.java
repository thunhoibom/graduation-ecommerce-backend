package org.monostudio.jpa.services.crud.impl;

import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.monostudio.api.models.ImagePojo;
import org.monostudio.api.models.ProductVariantPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Image;
import org.monostudio.jpa.entities.ProductVariant;
import org.monostudio.jpa.entities.VariantImage;
import org.monostudio.jpa.repositories.ProductVariantsRepository;
import org.monostudio.jpa.repositories.VariantImagesRepository;
import org.monostudio.jpa.services.conversion.ProductVariantsConverterService;
import org.monostudio.jpa.services.crud.CrudGenericService;
import org.monostudio.jpa.services.crud.ImagesCrudService;
import org.monostudio.jpa.services.crud.ProductVariantsCrudService;
import org.monostudio.jpa.services.patch.ProductVariantsPatchService;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Transactional
@Service
public class ProductVariantsCrudServiceImpl
    extends CrudGenericService<ProductVariantPojo, ProductVariant>
    implements ProductVariantsCrudService {

    private final ProductVariantsRepository productVariantsRepository;
    private final VariantImagesRepository variantImagesRepository;
    private final ImagesCrudService imagesCrudService;
    private final ProductVariantsConverterService productVariantsConverterService;

    private static final Logger logger = LoggerFactory.getLogger(ProductVariantsCrudServiceImpl.class);

    @Autowired
    public ProductVariantsCrudServiceImpl(
        ProductVariantsRepository productVariantsRepository,
        ProductVariantsConverterService productVariantsConverterService,
        ProductVariantsPatchService productVariantsPatchService,
        VariantImagesRepository variantImagesRepository,
        ImagesCrudService imagesCrudService
    ) {
        super(productVariantsRepository, productVariantsConverterService, productVariantsPatchService);
        this.productVariantsRepository = productVariantsRepository;
        this.productVariantsConverterService = productVariantsConverterService;
        this.variantImagesRepository = variantImagesRepository;
        this.imagesCrudService = imagesCrudService;
    }

    @Override
    @Transactional
    public ProductVariantPojo create(ProductVariantPojo input) throws BadInputException, EntityExistsException {
        this.validateInputPojoBeforeCreation(input);
        ProductVariant prepared = productVariantsConverterService.convertToNewEntity(input);
        ProductVariant persistent = productVariantsRepository.saveAndFlush(prepared);
        ProductVariantPojo target = productVariantsConverterService.convertToPojo(persistent);

        // Handle variant images
        Collection<ImagePojo> inputImages = input.getImages();
        if (inputImages != null && !inputImages.isEmpty()) {
            List<VariantImage> preparedImages = this.makeVariantImageRelationships(persistent, inputImages);
            List<VariantImage> persistentImages = variantImagesRepository.saveAll(preparedImages);
            target.setImages(productVariantsConverterService.convertVariantImagesToPojo(persistentImages));
            target.setPrimaryImageUrl(productVariantsConverterService.extractPrimaryImageUrl(persistentImages));
        }

        return target;
    }

    @Override
    @Transactional
    public Optional<ProductVariantPojo> update(ProductVariantPojo input, Long id)
        throws EntityNotFoundException, BadInputException {
        ProductVariant prepared = productVariantsConverterService.convertToNewEntity(input);
        prepared.setId(id);
        ProductVariant persistent = productVariantsRepository.saveAndFlush(prepared);
        ProductVariantPojo target = productVariantsConverterService.convertToPojo(persistent);

        // Replace variant images (delete-then-insert)
        variantImagesRepository.deleteByVariantId(id);
        Collection<ImagePojo> inputImages = input.getImages();
        if (inputImages != null && !inputImages.isEmpty()) {
            List<VariantImage> preparedImages = this.makeVariantImageRelationships(persistent, inputImages);
            List<VariantImage> persistentImages = variantImagesRepository.saveAll(preparedImages);
            target.setImages(productVariantsConverterService.convertVariantImagesToPojo(persistentImages));
            target.setPrimaryImageUrl(productVariantsConverterService.extractPrimaryImageUrl(persistentImages));
        }

        return Optional.of(target);
    }

    @Override
    @Transactional
    public ProductVariantPojo readOne(Predicate filters) throws EntityNotFoundException {
        Optional<ProductVariant> entity = productVariantsRepository.findOne(filters);
        if (entity.isEmpty()) {
            throw new EntityNotFoundException(ITEM_NOT_FOUND);
        }
        ProductVariant found = entity.get();
        ProductVariantPojo target = productVariantsConverterService.convertToPojo(found);

        // Populate variant images
        List<VariantImage> variantImages = variantImagesRepository.deepFindByVariantId(found.getId());
        target.setImages(productVariantsConverterService.convertVariantImagesToPojo(variantImages));
        target.setPrimaryImageUrl(productVariantsConverterService.extractPrimaryImageUrl(variantImages));

        return target;
    }

    @Override
    public Optional<ProductVariant> getExisting(ProductVariantPojo input) throws BadInputException {
        String sku = input.getSku();
        if (sku == null || sku.isBlank()) {
            throw new BadInputException("Invalid variant SKU");
        }
        return productVariantsRepository.findBySku(sku);
    }

    /**
     * Creates transient VariantImage entities. Does NOT persist.
     * First image is automatically set as primary if none is explicitly marked.
     */
    private List<VariantImage> makeVariantImageRelationships(ProductVariant variant, Collection<ImagePojo> inputImages) {
        List<VariantImage> allRelationships = new ArrayList<>();
        int order = 0;
        for (ImagePojo img : inputImages) {
            try {
                Optional<Image> match = imagesCrudService.getExisting(img);
                Image image = match.orElseGet(() -> Image.builder()
                    .code(img.getCode())
                    .filename(img.getFilename())
                    .url(img.getUrl())
                    .altText(img.getAltText())
                    .mimeType(img.getMimeType())
                    .width(img.getWidth())
                    .height(img.getHeight())
                    .fileSize(img.getFileSize())
                    .build());
                VariantImage relationship = VariantImage.builder()
                    .variant(variant)
                    .image(image)
                    .sortOrder(order)
                    .isPrimary(order == 0)
                    .build();
                allRelationships.add(relationship);
                order++;
            } catch (BadInputException ex) {
                logger.debug("An image was not linked to variant with SKU '{}'", variant.getSku());
            }
        }
        return allRelationships;
    }
}
