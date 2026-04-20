package org.monostudio.api.controllers;

import com.querydsl.core.types.OrderSpecifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.monostudio.api.DataCrudGenericController;
import org.monostudio.api.models.BulkOperationResult;
import org.monostudio.api.models.DataPagePojo;
import org.monostudio.api.models.ProductCsvImportResult;
import org.monostudio.api.models.ProductVariantPojo;
import org.monostudio.api.services.PaginationService;
import org.monostudio.api.services.VariantsBulkService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.ProductVariant;
import org.monostudio.jpa.services.SortSpecParserService;
import org.monostudio.jpa.services.crud.ProductVariantsCrudService;
import org.monostudio.jpa.services.predicates.ProductVariantsPredicateService;
import org.monostudio.jpa.sortspecs.ProductVariantsSortSpec;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/data/product-variants")
@Tag(name = "Product variants management")
public class DataProductVariantsController
    extends DataCrudGenericController<ProductVariantPojo, ProductVariant> {

    private final VariantsBulkService variantsBulkService;

    @Autowired
    public DataProductVariantsController(
        PaginationService paginationService,
        SortSpecParserService sortService,
        ProductVariantsCrudService crudService,
        ProductVariantsPredicateService predicateService,
        VariantsBulkService variantsBulkService
    ) {
        super(paginationService, sortService, crudService, predicateService);
        this.variantsBulkService = variantsBulkService;
    }

    @Override
    @GetMapping
    @Operation(summary = "List product variants.")
    public DataPagePojo<ProductVariantPojo> readMany(@RequestParam Map<String, String> allRequestParams) {
        return super.readMany(allRequestParams);
    }

    @Override
    @PostMapping
    @Operation(summary = "Define a new product variant.")
    @ResponseStatus(CREATED)
    @PreAuthorize("hasAuthority('products:create')")

    public void create(@RequestBody @Valid ProductVariantPojo input)

        throws BadInputException, EntityExistsException {
        crudService.create(input);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace product variant data.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('products:update')")

    public void update(@RequestBody @Valid ProductVariantPojo input, @PathVariable Long id)

        throws BadInputException, EntityNotFoundException {
        crudService.update(input, id);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update parts of product variant data.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('products:update')")

    public void partialUpdate(
        @RequestBody Map<String, Object> input,
        @PathVariable Long id
    ) throws BadInputException, EntityNotFoundException {
        crudService.partialUpdate(input, id)
            .orElseThrow(() -> new EntityNotFoundException("No element was found to update"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove product variants.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('products:delete')")

    public void delete(@PathVariable Long id)
        throws EntityNotFoundException {
        crudService.delete(id);
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
    public BulkOperationResult bulkActivate(@RequestBody List<Long> ids) {
        return variantsBulkService.bulkActivate(ids);
    }

    /**
     * Bulk-deactivate (disable) multiple variants at once.
     */
    @PostMapping("/bulk-deactivate")
    @Operation(summary = "Bulk-deactivate variants")
    @PreAuthorize("hasAuthority('products:update')")
    public BulkOperationResult bulkDeactivate(@RequestBody List<Long> ids) {
        return variantsBulkService.bulkDeactivate(ids);
    }

    /**
     * Bulk-delete multiple variants at once.
     */
    @PostMapping("/bulk-delete")
    @Operation(summary = "Bulk-delete variants")
    @PreAuthorize("hasAuthority('products:delete')")
    public BulkOperationResult bulkDeleteVariants(@RequestBody List<Long> ids) {
        return variantsBulkService.bulkDelete(ids);
    }

    /**
     * Import product variants from a CSV file.
     * Each row creates (or updates if SKU exists) one ProductVariant.
     */
    @PostMapping(value = "/import", consumes = "multipart/form-data")
    @Operation(summary = "Import variants from CSV")
    @PreAuthorize("hasAuthority('products:create')")
    public ProductCsvImportResult importVariants(
        @RequestPart("file") org.springframework.web.multipart.MultipartFile file
    ) throws IOException {
        return variantsBulkService.importVariants(file);
    }
}
