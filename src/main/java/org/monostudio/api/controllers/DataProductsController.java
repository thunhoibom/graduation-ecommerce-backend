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
import org.monostudio.api.models.ProductPojo;
import org.monostudio.api.services.BulkOperationsService;
import org.monostudio.api.services.PaginationService;
import org.monostudio.api.services.ProductsBulkService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Product;
import org.monostudio.jpa.entities.ProductStatus;
import org.monostudio.jpa.repositories.ProductsRepository;
import org.monostudio.jpa.services.SortSpecParserService;
import org.monostudio.jpa.services.crud.ProductsCrudService;
import org.monostudio.jpa.services.predicates.ProductsPredicateService;
import org.monostudio.jpa.sortspecs.ProductsSortSpec;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/data/products")
@Tag(name = "Products management")
public class DataProductsController
    extends DataCrudGenericController<ProductPojo, Product> {

    private final ProductsRepository productsRepository;
    private final ProductsBulkService productsBulkService;
    private final BulkOperationsService bulkOperationsService;

    @Autowired
    public DataProductsController(
        PaginationService paginationService,
        SortSpecParserService sortService,
        ProductsCrudService crudService,
        ProductsPredicateService predicateService,
        ProductsRepository productsRepository,
        ProductsBulkService productsBulkService,
        BulkOperationsService bulkOperationsService
    ) {
        super(paginationService, sortService, crudService, predicateService);
        this.productsRepository = productsRepository;
        this.productsBulkService = productsBulkService;
        this.bulkOperationsService = bulkOperationsService;
    }

    @Override
    @GetMapping
    @Operation(summary = "List products.")
    public DataPagePojo<ProductPojo> readMany(@RequestParam Map<String, String> allRequestParams) {
        return super.readMany(allRequestParams);
    }

    @Override
    @PostMapping
    @Operation(summary = "Define new products.")
    @ResponseStatus(CREATED)
    @PreAuthorize("hasAuthority('products:create')")
    public void create(@RequestBody ProductPojo input)
        throws BadInputException, EntityExistsException {
        crudService.create(input);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace products data.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('products:update')")
    public void update(@RequestBody ProductPojo input, @PathVariable Long id)
        throws BadInputException, EntityNotFoundException {
        crudService.update(input, id);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update parts of products data.")
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
    @Operation(summary = "Remove products.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('products:delete')")
    public void delete(@PathVariable Long id)
        throws EntityNotFoundException {
        crudService.delete(id);
    }

    @Override
    protected Map<String, OrderSpecifier<?>> getOrderSpecMap() {
        return ProductsSortSpec.ORDER_SPEC_MAP;
    }

    /**
     * Publish a draft product — makes it visible to customers.
     *
     * @param id Product ID
     * @return Updated ProductPojo with status PUBLISHED
     */
    @PatchMapping("/{id}/publish")
    @Operation(summary = "Publish a draft product — makes it visible to customers")
    @PreAuthorize("hasAuthority('products:update')")
    public ProductPojo publishProduct(@PathVariable Long id)
        throws EntityNotFoundException {
        Product product = productsRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
        product.setStatus(ProductStatus.PUBLISHED);
        productsRepository.saveAndFlush(product);
        return crudService.findById(id);
    }

    /**
     * Unpublish a product — hides it from customers (discontinued, seasonal, etc.).
     * The product remains in the system for existing orders and admin visibility.
     *
     * @param id Product ID
     * @return Updated ProductPojo with status UNLISTED
     */
    @PatchMapping("/{id}/unpublish")
    @Operation(summary = "Unpublish a product — hides it from customers (discontinued/seasonal)")
    @PreAuthorize("hasAuthority('products:update')")
    public ProductPojo unpublishProduct(@PathVariable Long id)
        throws EntityNotFoundException {
        Product product = productsRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
        product.setStatus(ProductStatus.UNLISTED);
        productsRepository.saveAndFlush(product);
        return crudService.findById(id);
    }

    /**
     * Revert a product back to draft — removes it from public visibility.
     *
     * @param id Product ID
     * @return Updated ProductPojo with status DRAFT
     */
    @PatchMapping("/{id}/revert-to-draft")
    @Operation(summary = "Revert a product back to draft — removes from public visibility")
    @PreAuthorize("hasAuthority('products:update')")
    public ProductPojo revertToDraft(@PathVariable Long id)
        throws EntityNotFoundException {
        Product product = productsRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
        product.setStatus(ProductStatus.DRAFT);
        productsRepository.saveAndFlush(product);
        return crudService.findById(id);
    }

    // ─── Bulk Operations ────────────────────────────────────────────────────────

    /**
     * Export all products as a CSV file.
     *
     * @param categoryCode Optional category code to filter products
     * @return CSV file as binary download
     * @throws IOException On export error
     */
    @GetMapping("/export")
    @Operation(summary = "Export products as CSV")
    @PreAuthorize("hasAuthority('products:read')")
    public byte[] exportProducts(
        @RequestParam(required = false) String categoryCode
    ) throws IOException {
        byte[] csv = productsBulkService.exportProducts(categoryCode);
        return csv;
    }

    /**
     * Bulk-publish multiple products at once.
     *
     * @param ids Product IDs to publish
     * @return Result with success/error counts
     */
    @PostMapping("/bulk-publish")
    @Operation(summary = "Bulk-publish products")
    @PreAuthorize("hasAuthority('products:update')")
    public BulkOperationResult bulkPublish(@RequestBody List<Long> ids)
        throws BadInputException {
        return bulkOperationsService.bulkPublish(ids);
    }

    /**
     * Bulk-unpublish multiple products at once.
     *
     * @param ids Product IDs to unpublish
     * @return Result with success/error counts
     */
    @PostMapping("/bulk-unpublish")
    @Operation(summary = "Bulk-unpublish products")
    @PreAuthorize("hasAuthority('products:update')")
    public BulkOperationResult bulkUnpublish(@RequestBody List<Long> ids)
        throws BadInputException {
        return bulkOperationsService.bulkUnpublish(ids);
    }

    /**
     * Bulk-delete multiple products at once.
     *
     * @param ids Product IDs to delete
     * @return Result with success/error counts
     */
    @PostMapping("/bulk-delete")
    @Operation(summary = "Bulk-delete products")
    @PreAuthorize("hasAuthority('products:delete')")
    public BulkOperationResult bulkDelete(@RequestBody List<Long> ids)
        throws BadInputException {
        return bulkOperationsService.bulkDelete(ids);
    }

    /**
     * Import products from a CSV file.
     *
     * @param file CSV file uploaded by admin
     * @return Import result with success/error counts and per-row error details
     * @throws IOException On file read error
     */
    @PostMapping(value = "/import", consumes = "multipart/form-data")
    @Operation(summary = "Import products from CSV")
    @PreAuthorize("hasAuthority('products:create')")
    public ProductCsvImportResult importProducts(
        @RequestPart("file") org.springframework.web.multipart.MultipartFile file
    ) throws IOException {
        return productsBulkService.importProducts(file);
    }
}
