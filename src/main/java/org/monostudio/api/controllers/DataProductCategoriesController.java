package org.monostudio.api.controllers;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
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
import org.monostudio.api.models.ProductCategoryPojo;
import org.monostudio.api.services.PaginationService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.ProductCategory;
import org.monostudio.jpa.services.SortSpecParserService;
import org.monostudio.jpa.services.crud.ProductCategoriesCrudService;
import org.monostudio.jpa.services.crud.ProductsCrudService;
import org.monostudio.api.models.ProductPojo;
import org.monostudio.jpa.services.predicates.ProductCategoriesPredicateService;
import org.monostudio.jpa.services.predicates.ProductsPredicateService;
import org.monostudio.jpa.sortspecs.ProductCategoriesSortSpec;
import org.monostudio.jpa.sortspecs.ProductsSortSpec;
import org.monostudio.config.cache.CacheNames;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.Map;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/data/product_categories")
@Tag(name = "Product Categories management")
public class DataProductCategoriesController
    extends DataCrudGenericController<ProductCategoryPojo, ProductCategory> {

    private final ProductsCrudService productsCrudService;
    private final ProductsPredicateService productsPredicateService;

    @Autowired
    public DataProductCategoriesController(
        PaginationService paginationService,
        SortSpecParserService sortService,
        ProductCategoriesCrudService crudService,
        ProductCategoriesPredicateService predicateService,
        ProductsCrudService productsCrudService,
        ProductsPredicateService productsPredicateService
    ) {
        super(paginationService, sortService, crudService, predicateService);
        this.productsCrudService = productsCrudService;
        this.productsPredicateService = productsPredicateService;
    }

    @Override
    @GetMapping
    @Operation(summary = "List product categories.")
    public DataPagePojo<ProductCategoryPojo> readMany(@RequestParam Map<String, String> allRequestParams) {
        if (allRequestParams==null || allRequestParams.isEmpty()) {
            allRequestParams = Map.of("parentId", "");
        }
        return super.readMany(allRequestParams);
    }

    @GetMapping("/{code:^[a-zA-Z0-9\\-]+$}")
    @Operation(summary = "Get product category by code.")
    public ProductCategoryPojo readOne(@PathVariable String code) {
        return crudService.readOne(predicateService.parseMap(Map.of("code", code)));
    }

    @GetMapping("/{code:^[a-zA-Z0-9\\-]+$}/products")
    @Operation(summary = "List products for a category code.")
    public DataPagePojo<ProductPojo> listProductsByCategory(
        @PathVariable String code,
        @RequestParam Map<String, String> params
    ) {
        int pageIndex = paginationService.determineRequestedPageIndex(params);
        int pageSize = paginationService.determineRequestedPageSize(params);
        params = new java.util.HashMap<>(params);
        params.put("categoryCode", code);
        Predicate filters = productsPredicateService.parseMap(params);
        org.springframework.data.domain.Sort order = sortService.parse(ProductsSortSpec.ORDER_SPEC_MAP, params);
        return productsCrudService.readMany(pageIndex, pageSize, order, filters);
    }

    @Override
    @PostMapping
    @Operation(summary = "Define new product categories.")
    @ResponseStatus(CREATED)
    @PreAuthorize("hasAuthority('product_categories:create')")
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.PUBLIC_CATEGORY_TREE, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.PUBLIC_CATEGORY_BY_CODE, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.PUBLIC_PRODUCTS_LIST, allEntries = true)
    })
    public void create(@RequestBody ProductCategoryPojo input)
        throws BadInputException, EntityExistsException {
        crudService.create(input);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace product categories data.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('product_categories:update')")
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.PUBLIC_CATEGORY_TREE, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.PUBLIC_CATEGORY_BY_CODE, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.PUBLIC_PRODUCTS_LIST, allEntries = true)
    })
    public void update(@RequestBody ProductCategoryPojo input, @PathVariable Long id)
        throws BadInputException, EntityNotFoundException {
        crudService.update(input, id);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update parts of product categories data.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('product_categories:update')")
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.PUBLIC_CATEGORY_TREE, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.PUBLIC_CATEGORY_BY_CODE, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.PUBLIC_PRODUCTS_LIST, allEntries = true)
    })
    public void partialUpdate(
        @RequestBody Map<String, Object> input,
        @PathVariable Long id
    ) throws BadInputException, EntityNotFoundException {
        crudService.partialUpdate(input, id)
            .orElseThrow(() -> new EntityNotFoundException("No element was found to update"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove product categories.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('product_categories:delete')")
    @Caching(evict = {
        @CacheEvict(cacheNames = CacheNames.PUBLIC_CATEGORY_TREE, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.PUBLIC_CATEGORY_BY_CODE, allEntries = true),
        @CacheEvict(cacheNames = CacheNames.PUBLIC_PRODUCTS_LIST, allEntries = true)
    })
    public void delete(@PathVariable Long id)
        throws EntityNotFoundException {
        crudService.delete(id);
    }

    @Override
    protected Map<String, OrderSpecifier<?>> getOrderSpecMap() {
        return ProductCategoriesSortSpec.ORDER_SPEC_MAP;
    }
}
