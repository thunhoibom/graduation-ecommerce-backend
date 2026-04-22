package org.monostudio.api.controllers;

import com.querydsl.core.types.OrderSpecifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.monostudio.api.DataCrudGenericController;
import org.monostudio.api.models.BulkOperationResult;
import org.monostudio.api.models.DataPagePojo;
import org.monostudio.api.models.ProductCsvImportResult;
import org.monostudio.api.models.ProductVariantPojo;
import org.monostudio.api.models.VariantBulkUpdateRequest;
import org.monostudio.api.services.PaginationService;
import org.monostudio.api.services.ProductAuditLogService;
import org.monostudio.api.services.VariantsBulkService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.ProductAuditLog;
import org.monostudio.jpa.entities.ProductVariant;
import org.monostudio.jpa.repositories.ProductsRepository;
import org.monostudio.jpa.services.SortSpecParserService;
import org.monostudio.jpa.services.crud.ProductVariantsCrudService;
import org.monostudio.jpa.services.predicates.ProductVariantsPredicateService;
import org.monostudio.jpa.sortspecs.ProductVariantsSortSpec;
import org.monostudio.config.cache.CacheNames;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/data/product-variants")
@Tag(name = "Product variants management")
public class DataProductVariantsController
    extends DataCrudGenericController<ProductVariantPojo, ProductVariant> {

    private final VariantsBulkService variantsBulkService;
    private final ProductAuditLogService productAuditLogService;
    private final ProductsRepository productsRepository;

    @Autowired
    public DataProductVariantsController(
        PaginationService paginationService,
        SortSpecParserService sortService,
        ProductVariantsCrudService crudService,
        ProductVariantsPredicateService predicateService,
        VariantsBulkService variantsBulkService,
        ProductAuditLogService productAuditLogService,
        ProductsRepository productsRepository
    ) {
        super(paginationService, sortService, crudService, predicateService);
        this.variantsBulkService = variantsBulkService;
        this.productAuditLogService = productAuditLogService;
        this.productsRepository = productsRepository;
    }

    @Override
    @GetMapping
    @Operation(summary = "List product variants.")
    public DataPagePojo<ProductVariantPojo> readMany(@RequestParam Map<String, String> allRequestParams) {
        return super.readMany(allRequestParams);
    }

    @PostMapping
    @Operation(summary = "Define a new product variant.")
    @ResponseStatus(CREATED)
    @PreAuthorize("hasAuthority('products:create')")
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.PUBLIC_PRODUCTS_LIST, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.PUBLIC_PRODUCT_DETAIL, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_LOW_STOCK, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_DASHBOARD_STATS, allEntries = true)
    })

    public void create(@RequestBody @Valid ProductVariantPojo input, Principal principal)

        throws BadInputException, EntityExistsException {
        ProductVariantPojo created = crudService.create(input);
        auditVariantChange("VARIANT_CREATE", null, created, principal, null);
    }

    @Override
    public void create(@Valid ProductVariantPojo input)
        throws BadInputException, EntityExistsException {
        create(input, null);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace product variant data.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('products:update')")
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.PUBLIC_PRODUCTS_LIST, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.PUBLIC_PRODUCT_DETAIL, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_LOW_STOCK, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_DASHBOARD_STATS, allEntries = true)
    })

    public void update(@RequestBody @Valid ProductVariantPojo input, @PathVariable Long id, Principal principal)

        throws BadInputException, EntityNotFoundException {
        ProductVariantPojo before = safeFindById(id);
        ProductVariantPojo after = crudService.update(input, id)
            .orElseThrow(() -> new EntityNotFoundException("No element was found to update"));
        auditVariantChange("VARIANT_UPDATE", before, after, principal, null);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update parts of product variant data.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('products:update')")
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.PUBLIC_PRODUCTS_LIST, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.PUBLIC_PRODUCT_DETAIL, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_LOW_STOCK, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_DASHBOARD_STATS, allEntries = true)
    })

    public void partialUpdate(
        @RequestBody Map<String, Object> input,
        @PathVariable Long id,
        Principal principal
    ) throws BadInputException, EntityNotFoundException {
        ProductVariantPojo before = safeFindById(id);
        crudService.partialUpdate(input, id)
            .orElseThrow(() -> new EntityNotFoundException("No element was found to update"));
        ProductVariantPojo after = safeFindById(id);
        auditVariantChange("VARIANT_PATCH", before, after, principal, null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove product variants.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('products:delete')")
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.PUBLIC_PRODUCTS_LIST, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.PUBLIC_PRODUCT_DETAIL, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_LOW_STOCK, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_DASHBOARD_STATS, allEntries = true)
    })

    public void delete(@PathVariable Long id, Principal principal)
        throws EntityNotFoundException {
        ProductVariantPojo before = safeFindById(id);
        crudService.delete(id);
        auditVariantChange("VARIANT_DELETE", before, null, principal, null);
    }

    @Override
    protected Map<String, OrderSpecifier<?>> getOrderSpecMap() {
        return ProductVariantsSortSpec.ORDER_SPEC_MAP;
    }

    // ─── Bulk Operations ────────────────────────────────────────────────────────

    /**
     * Export all product variants as a CSV file.
     */
    @GetMapping("/export")
    @Operation(summary = "Export variants as CSV")
    @PreAuthorize("hasAuthority('products:read')")
    public byte[] exportVariants(
        @RequestParam(required = false) String productBarcode
    ) throws IOException {
        return variantsBulkService.exportVariants(productBarcode);
    }

    /**
     * Bulk-activate (enable) multiple variants at once.
     */
    @PostMapping("/bulk-activate")
    @Operation(summary = "Bulk-activate variants")
    @PreAuthorize("hasAuthority('products:update')")
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.PUBLIC_PRODUCTS_LIST, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.PUBLIC_PRODUCT_DETAIL, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_LOW_STOCK, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_DASHBOARD_STATS, allEntries = true)
    })
    public BulkOperationResult bulkActivate(@RequestBody List<Long> ids, Principal principal) {
        String correlationId = "variants-bulk-activate-" + UUID.randomUUID();
        Map<Long, ProductVariantPojo> before = snapshotVariants(ids);
        BulkOperationResult result = variantsBulkService.bulkActivate(ids);
        for (Long id : ids) {
            ProductVariantPojo beforeItem = before.get(id);
            ProductVariantPojo afterItem = safeFindByIdOrNull(id);
            if (beforeItem != null || afterItem != null) {
                auditVariantChange("VARIANT_BULK_ACTIVATE", beforeItem, afterItem, principal, correlationId);
            }
        }
        return result;
    }

    /**
     * Bulk-deactivate (disable) multiple variants at once.
     */
    @PostMapping("/bulk-deactivate")
    @Operation(summary = "Bulk-deactivate variants")
    @PreAuthorize("hasAuthority('products:update')")
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.PUBLIC_PRODUCTS_LIST, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.PUBLIC_PRODUCT_DETAIL, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_LOW_STOCK, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_DASHBOARD_STATS, allEntries = true)
    })
    public BulkOperationResult bulkDeactivate(@RequestBody List<Long> ids, Principal principal) {
        String correlationId = "variants-bulk-deactivate-" + UUID.randomUUID();
        Map<Long, ProductVariantPojo> before = snapshotVariants(ids);
        BulkOperationResult result = variantsBulkService.bulkDeactivate(ids);
        for (Long id : ids) {
            ProductVariantPojo beforeItem = before.get(id);
            ProductVariantPojo afterItem = safeFindByIdOrNull(id);
            if (beforeItem != null || afterItem != null) {
                auditVariantChange("VARIANT_BULK_DEACTIVATE", beforeItem, afterItem, principal, correlationId);
            }
        }
        return result;
    }

    /**
     * Bulk-delete multiple variants at once.
     */
    @PostMapping("/bulk-delete")
    @Operation(summary = "Bulk-delete variants")
    @PreAuthorize("hasAuthority('products:delete')")
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.PUBLIC_PRODUCTS_LIST, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.PUBLIC_PRODUCT_DETAIL, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_LOW_STOCK, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_DASHBOARD_STATS, allEntries = true)
    })
    public BulkOperationResult bulkDeleteVariants(@RequestBody List<Long> ids, Principal principal) {
        String correlationId = "variants-bulk-delete-" + UUID.randomUUID();
        Map<Long, ProductVariantPojo> before = snapshotVariants(ids);
        BulkOperationResult result = variantsBulkService.bulkDelete(ids);
        for (Long id : ids) {
            ProductVariantPojo beforeItem = before.get(id);
            if (beforeItem != null) {
                auditVariantChange("VARIANT_BULK_DELETE", beforeItem, null, principal, correlationId);
            }
        }
        return result;
    }

    @PostMapping("/bulk-update")
    @Operation(summary = "Bulk-update variant fields")
    @PreAuthorize("hasAuthority('products:update')")
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.PUBLIC_PRODUCTS_LIST, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.PUBLIC_PRODUCT_DETAIL, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_LOW_STOCK, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_DASHBOARD_STATS, allEntries = true)
    })
    public BulkOperationResult bulkUpdateVariants(@RequestBody VariantBulkUpdateRequest request, Principal principal) {
        List<Long> ids = request != null ? request.getIds() : null;
        String correlationId = "variants-bulk-update-" + UUID.randomUUID();
        Map<Long, ProductVariantPojo> before = snapshotVariants(ids);
        BulkOperationResult result = variantsBulkService.bulkUpdate(
            ids,
            request != null ? request.getPriceModifier() : null,
            request != null ? request.getCurrentStock() : null,
            request != null ? request.getActive() : null
        );
        if (ids != null) {
            for (Long id : ids) {
                ProductVariantPojo beforeItem = before.get(id);
                ProductVariantPojo afterItem = safeFindByIdOrNull(id);
                if (beforeItem != null || afterItem != null) {
                    auditVariantChange("VARIANT_BULK_UPDATE", beforeItem, afterItem, principal, correlationId);
                }
            }
        }
        return result;
    }

    /**
     * Import product variants from a CSV file.
     * Each row creates (or updates if SKU exists) one ProductVariant.
     */
    @PostMapping(value = "/import", consumes = "multipart/form-data")
    @Operation(summary = "Import variants from CSV")
    @PreAuthorize("hasAuthority('products:create')")
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.PUBLIC_PRODUCTS_LIST, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.PUBLIC_PRODUCT_DETAIL, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_LOW_STOCK, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.ADMIN_DASHBOARD_STATS, allEntries = true)
    })
    public ProductCsvImportResult importVariants(
        @RequestPart("file") org.springframework.web.multipart.MultipartFile file,
        Principal principal
    ) throws IOException {
        ProductCsvImportResult result = variantsBulkService.importVariants(file);
        productAuditLogService.recordBestEffort(new ProductAuditLogService.AuditEvent(
            "VARIANT_IMPORT",
            ProductAuditLog.EntityType.VARIANT,
            0L,
            null,
            null,
            null,
            null,
            result,
            "Imported variants from CSV",
            actor(principal),
            "ADMIN_API",
            "variants-import-" + UUID.randomUUID()
        ));
        return result;
    }

    private Map<Long, ProductVariantPojo> snapshotVariants(List<Long> ids) {
        Map<Long, ProductVariantPojo> snapshots = new HashMap<>();
        if (ids == null) {
            return snapshots;
        }
        for (Long id : ids) {
            ProductVariantPojo found = safeFindByIdOrNull(id);
            if (found != null) {
                snapshots.put(id, found);
            }
        }
        return snapshots;
    }

    private ProductVariantPojo safeFindById(Long id) {
        return crudService.findById(id);
    }

    private ProductVariantPojo safeFindByIdOrNull(Long id) {
        try {
            return safeFindById(id);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void auditVariantChange(
        String action,
        ProductVariantPojo before,
        ProductVariantPojo after,
        Principal principal,
        String correlationId
    ) {
        ProductVariantPojo anchor = after != null ? after : before;
        if (anchor == null || anchor.getId() == null) {
            return;
        }
        Long productId = null;
        if (anchor.getProductBarcode() != null) {
            productId = productsRepository.findByBarcode(anchor.getProductBarcode())
                .map(org.monostudio.jpa.entities.Product::getId)
                .orElse(null);
        }
        productAuditLogService.recordBestEffort(new ProductAuditLogService.AuditEvent(
            action,
            ProductAuditLog.EntityType.VARIANT,
            anchor.getId(),
            productId,
            anchor.getId(),
            anchor.getSku(),
            before,
            after,
            "Variant change: " + action,
            actor(principal),
            "ADMIN_API",
            correlationId
        ));
    }

    private String actor(Principal principal) {
        return principal != null ? principal.getName() : "system";
    }
}
