package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.monostudio.api.models.DataPagePojo;
import org.monostudio.api.models.ProductPojo;
import org.monostudio.api.services.PaginationService;
import org.monostudio.jpa.entities.ProductStatus;
import org.monostudio.jpa.services.SortSpecParserService;
import org.monostudio.jpa.services.crud.ProductsCrudService;
import org.monostudio.jpa.services.predicates.ProductsPredicateService;
import org.monostudio.jpa.sortspecs.ProductsSortSpec;

import jakarta.persistence.EntityNotFoundException;
import org.monostudio.search.models.ProductDocument;
import org.monostudio.search.services.SearchService;
import org.monostudio.config.cache.CacheNames;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Public product browsing endpoints.
 * All list/search endpoints automatically filter by status=PUBLISHED.
 * Customers can only see live products.
 */
@RestController
@RequestMapping("/api/public/products")
@Tag(name = "Public Product Data")
public class PublicProductsController {
    private final ProductsCrudService productsCrudService;
    private final ProductsPredicateService productsPredicateService;
    private final PaginationService paginationService;
    private final SortSpecParserService sortService;
    private final SearchService searchService;

    @Autowired
    public PublicProductsController(
        ProductsCrudService productsCrudService,
        ProductsPredicateService productsPredicateService,
        PaginationService paginationService,
        SortSpecParserService sortService,
        SearchService searchService
    ) {
        this.productsCrudService = productsCrudService;
        this.productsPredicateService = productsPredicateService;
        this.paginationService = paginationService;
        this.sortService = sortService;
        this.searchService = searchService;
    }

    /**
     * List or search published products for customer browsing.
     * Automatically filters by status=PUBLISHED — customers cannot see DRAFT or UNLISTED products.
     */
    @GetMapping
    @Operation(summary = "List or search published products")
    @Cacheable(cacheNames = CacheNames.PUBLIC_PRODUCTS_LIST, key = "@cacheKeyBuilder.fromParams(#allRequestParams)")
    public DataPagePojo<ProductPojo> listProducts(@RequestParam Map<String, String> allRequestParams) {
        // Always enforce status=PUBLISHED for public listings
        Map<String, String> params = new HashMap<>();
        if (allRequestParams != null) {
            params.putAll(allRequestParams);
        }
        params.put("status", ProductStatus.PUBLISHED.name());

        // Default sort: newest first
        if (!params.containsKey("sortBy") && !params.containsKey("order")) {
            params.put("sortBy", "id");
            params.put("order", "desc");
        }

        int pageIndex = paginationService.determineRequestedPageIndex(params);
        int pageSize = paginationService.determineRequestedPageSize(params);
        var sort = sortService.parse(ProductsSortSpec.ORDER_SPEC_MAP, params);
        var filters = productsPredicateService.parseMap(params);

        return productsCrudService.readMany(pageIndex, pageSize, sort, filters);
    }

    /**
     * Get a single published product by its barcode.
     * Returns 404 if the product does not exist OR if it is not PUBLISHED (hidden from customers).
     */
    @GetMapping("/{barcode}")
    @Operation(summary = "Get a published product by barcode")
    @Cacheable(cacheNames = CacheNames.PUBLIC_PRODUCT_DETAIL, key = "#barcode")
    public ProductPojo getProductByBarcode(@PathVariable String barcode) {
        // First check if product exists and is published
        Map<String, String> params = new HashMap<>();
        params.put("barcode", barcode);
        params.put("status", ProductStatus.PUBLISHED.name());

        try {
            return productsCrudService.readOne(productsPredicateService.parseMap(params));
        } catch (EntityNotFoundException ex) {
            // Re-throw as 404 — do not reveal whether product exists but is hidden
            throw new EntityNotFoundException("Product not found: " + barcode);
        }
    }

    /**
     * Search products using Elasticsearch for full-text search capabilities.
     * Supports pagination, price filtering, and sorting.
     */
    @GetMapping("/search")
    @Operation(summary = "Full-text search for products using Elasticsearch")
    public DataPagePojo<ProductDocument> searchProducts(@RequestParam Map<String, String> params) {
        String query = params.getOrDefault("q", "");
        int pageIndex = paginationService.determineRequestedPageIndex(params);
        int pageSize = paginationService.determineRequestedPageSize(params);
        
        Integer minPrice = null;
        if (params.containsKey("minPrice")) {
            try { minPrice = Integer.parseInt(params.get("minPrice")); } catch (Exception ignored) {}
        }
        Integer maxPrice = null;
        if (params.containsKey("maxPrice")) {
            try { maxPrice = Integer.parseInt(params.get("maxPrice")); } catch (Exception ignored) {}
        }

        String category = params.get("category");

        var sort = sortService.parse(ProductsSortSpec.ORDER_SPEC_MAP, params);
        
        return searchService.searchProducts(
                query,
                minPrice,
                maxPrice,
                category,
                ProductStatus.PUBLISHED.name(),
                pageIndex,
                pageSize,
                sort
        );
    }
}
