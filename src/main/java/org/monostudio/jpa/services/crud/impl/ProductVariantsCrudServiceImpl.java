package org.monostudio.jpa.services.crud.impl;

import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;

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
        validateUniqueCombination(prepared, null);
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
        ProductVariant existing = productVariantsRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException(ITEM_NOT_FOUND));
        ProductVariant prepared = productVariantsConverterService.applyChangesToExistingEntity(input, existing);
        validateUniqueCombination(prepared, id);
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
     * Override readMany to attach images in a single batch query (no N+1).
     * Uses deepReadAll for the unfiltered path (JOIN FETCHes product).
     * Uses repository predicate query for the filtered path.
     */
    @Override
    public org.monostudio.api.models.DataPagePojo<ProductVariantPojo> readMany(
            int pageIndex,
            int pageSize,
            @Nullable Sort order,
            @Nullable Predicate filters
    ) {
        Pageable pagination = (order == null)
            ? PageRequest.of(pageIndex, pageSize)
            : PageRequest.of(pageIndex, pageSize, order);

        Page<ProductVariant> page = (filters == null)
            ? productVariantsRepository.deepReadAll(pagination)
            : productVariantsRepository.findAll(filters, pagination);
        List<ProductVariant> variants = page.getContent();

        // Batch-load all variant images in a single round-trip (eliminates N+1)
        if (!variants.isEmpty()) {
            List<Long> variantIds = variants.stream()
                .map(ProductVariant::getId)
                .toList();
            List<VariantImage> allImages = variantImagesRepository.findByVariantIdIn(variantIds);
            Map<Long, List<VariantImage>> imagesByVariantId = allImages.stream()
                .collect(Collectors.groupingBy(vi -> vi.getVariant().getId()));

            List<ProductVariantPojo> pojoList = variants.stream().map(v -> {
                ProductVariantPojo pojo = productVariantsConverterService.convertToPojo(v);
                List<VariantImage> imgs = imagesByVariantId.getOrDefault(v.getId(), List.of());
                pojo.setImages(productVariantsConverterService.convertVariantImagesToPojo(imgs));
                pojo.setPrimaryImageUrl(productVariantsConverterService.extractPrimaryImageUrl(imgs));
                return pojo;
            }).toList();

            return new org.monostudio.api.models.DataPagePojo<>(
                pojoList, pageIndex, page.getTotalElements(), pageSize);
        }

        // Empty page — fall through to base behaviour
        return super.readMany(pageIndex, pageSize, order, filters);
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

    private void validateUniqueCombination(ProductVariant variant, Long excludeId) throws BadInputException {
        if (variant.getProduct() == null || variant.getProduct().getId() == null) {
            throw new BadInputException("Product is required for variant");
        }
        if (StringUtils.isBlank(variant.getSize())) {
            throw new BadInputException("Size is required for variant");
        }
        String normalizedColor = StringUtils.trimToEmpty(variant.getColor());

        boolean duplicated = productVariantsRepository.existsDuplicateCombination(
            variant.getProduct().getId(),
            variant.getSize(),
            normalizedColor,
            excludeId
        );
        if (duplicated) {
            String colorLabel = normalizedColor.isEmpty() ? "N/A" : normalizedColor;
            throw new BadInputException(
                "Duplicate variant combination for this product: size="
                    + variant.getSize()
                    + ", color="
                    + colorLabel
            );
        }
    }
}
