package org.monostudio.api.controllers;

import com.querydsl.core.types.Predicate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.monostudio.api.models.DataPagePojo;
import org.monostudio.api.models.ProductPojo;
import org.monostudio.api.services.PaginationService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Product;
import org.monostudio.jpa.entities.ProductList;
import org.monostudio.jpa.entities.ProductListItem;
import org.monostudio.jpa.entities.QProductList;
import org.monostudio.jpa.entities.QProductListItem;
import org.monostudio.jpa.repositories.ProductListItemsRepository;
import org.monostudio.jpa.repositories.ProductListsRepository;
import org.monostudio.jpa.services.SortSpecParserService;
import org.monostudio.jpa.services.conversion.ProductListItemsConverterService;
import org.monostudio.jpa.services.crud.ProductsCrudService;
import org.monostudio.jpa.services.predicates.ProductListItemsPredicateService;
import org.monostudio.jpa.sortspecs.ProductListItemsSortSpec;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/data/product_list_contents")
@Tag(name = "Product Lists management")
public class DataProductListContentsController {
    private static final String ITEM_NOT_FOUND = "Requested item(s) not found";
    private final PaginationService paginationService;
    private final SortSpecParserService sortService;
    private final ProductListItemsRepository listItemsRepository;
    private final ProductListsRepository listsRepository;
    private final ProductListItemsPredicateService listItemsPredicateService;
    private final ProductsCrudService productCrudService;
    private final ProductListItemsConverterService itemConverterService;

    @Autowired
    public DataProductListContentsController(
        PaginationService paginationService,
        SortSpecParserService sortService,
        ProductListItemsRepository listItemsRepository,
        ProductListsRepository listsRepository,
        ProductListItemsPredicateService listItemsPredicateService,
        ProductsCrudService productCrudService,
        ProductListItemsConverterService itemConverterService
    ) {
        this.paginationService = paginationService;
        this.sortService = sortService;
        this.listItemsRepository = listItemsRepository;
        this.listsRepository = listsRepository;
        this.listItemsPredicateService = listItemsPredicateService;
        this.productCrudService = productCrudService;
        this.itemConverterService = itemConverterService;
    }

    @GetMapping
    @Operation(summary = "View contents of product lists.")
    public DataPagePojo<ProductPojo> readContents(@RequestParam Map<String, String> requestParams)
        throws BadInputException, EntityNotFoundException {
        Optional<ProductList> match = this.fetchProductListByCode(requestParams);
        if (match.isEmpty()) {
            throw new EntityNotFoundException(ITEM_NOT_FOUND);
        }

        int pageIndex = paginationService.determineRequestedPageIndex(requestParams);
        int pageSize = paginationService.determineRequestedPageSize(requestParams);

        Pageable pagination;
        if (requestParams.containsKey("sortBy")) {
            Sort order = sortService.parse(ProductListItemsSortSpec.ORDER_SPEC_MAP, requestParams);
            pagination = PageRequest.of(pageIndex, pageSize, order);
        } else {
            pagination = PageRequest.of(pageIndex, pageSize);
        }

        Predicate predicate = listItemsPredicateService.parseMap(requestParams);
        Page<ProductListItem> listItems = listItemsRepository.findAll(predicate, pagination);
        List<ProductPojo> products = new ArrayList<>();
        for (ProductListItem item : listItems) {
            ProductPojo productPojo = itemConverterService.convertToPojo(item);
            products.add(productPojo);
        }
        long totalCount = listItemsRepository.count(QProductListItem.productListItem.list.id.eq(match.get().getId()));

        return new DataPagePojo<>(products, pageIndex, totalCount, pageSize);
    }

    @PostMapping
    @Operation(summary = "Add products to lists.")
    @ResponseStatus(CREATED)
    @PreAuthorize("hasAuthority('product_lists:contents')")
    public void addToContents(@Valid @RequestBody ProductPojo input,
                              @RequestParam Map<String, String> requestParams)
        throws BadInputException, EntityNotFoundException {
        Optional<ProductList> listMatch = this.fetchProductListByCode(requestParams);
        if (listMatch.isEmpty()) {
            throw new EntityNotFoundException(ITEM_NOT_FOUND);
        }

        Optional<Product> productMatch = productCrudService.getExisting(input);
        if (productMatch.isPresent()) {
            ProductListItem listItem = ProductListItem.builder()
                .list(listMatch.get())
                .product(productMatch.get())
                .build();
            if (!listItemsRepository.exists(Example.of(listItem))) {
                listItemsRepository.save(listItem);
            }
        }
    }

    @PutMapping
    @Operation(summary = "Fully replace contents of product lists.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('product_lists:contents')")
    public void updateContents(@RequestBody Collection<ProductPojo> input,
                               @RequestParam Map<String, String> requestParams)
        throws BadInputException, EntityNotFoundException {
        Optional<ProductList> listMatch = this.fetchProductListByCode(requestParams);
        if (listMatch.isEmpty()) {
            throw new EntityNotFoundException(ITEM_NOT_FOUND);
        }

        listItemsRepository.deleteByListId(listMatch.get().getId());
        for (ProductPojo p : input) {
            Optional<Product> productMatch = productCrudService.getExisting(p);
            if (productMatch.isPresent()) {
                ProductListItem listItem = ProductListItem.builder()
                    .list(listMatch.get())
                    .product(productMatch.get())
                    .build();
                if (!listItemsRepository.exists(Example.of(listItem))) {
                    listItemsRepository.save(listItem);
                }
            }
        }
    }

    @DeleteMapping
    @Operation(summary = "Remove products from lists.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('product_lists:contents')")
    public void deleteFromContents(@RequestParam Map<String, String> requestParams)
        throws BadInputException, EntityNotFoundException {
        Optional<ProductList> listMatch = this.fetchProductListByCode(requestParams);
        if (listMatch.isEmpty()) {
            throw new EntityNotFoundException(ITEM_NOT_FOUND);
        }

        Predicate predicate = listItemsPredicateService.parseMap(requestParams);
        listItemsRepository.deleteAll(listItemsRepository.findAll(predicate));
    }

    private Optional<ProductList> fetchProductListByCode(Map<String, String> requestParams) throws BadInputException {
        String listCode = requestParams.get("listCode");
        if (StringUtils.isBlank(listCode)) {
            throw new BadInputException("listCode query param is required");
        }
        return listsRepository.findOne(QProductList.productList.code.eq(listCode));
    }
}
