package org.monostudio.api.controllers;

import com.querydsl.core.types.OrderSpecifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.monostudio.api.DataCrudGenericController;
import org.monostudio.api.models.DataPagePojo;
import org.monostudio.api.models.ProductPojo;
import org.monostudio.api.services.PaginationService;
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
import java.util.Map;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/data/products")
@Tag(name = "Products management")
public class DataProductsController
    extends DataCrudGenericController<ProductPojo, Product> {

    private final ProductsRepository productsRepository;

    @Autowired
    public DataProductsController(
        PaginationService paginationService,
        SortSpecParserService sortService,
        ProductsCrudService crudService,
        ProductsPredicateService predicateService,
        ProductsRepository productsRepository
    ) {
        super(paginationService, sortService, crudService, predicateService);
        this.productsRepository = productsRepository;
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
}
