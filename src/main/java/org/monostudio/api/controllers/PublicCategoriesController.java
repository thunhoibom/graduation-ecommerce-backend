package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.monostudio.api.models.CategoryTreePojo;
import org.monostudio.api.models.ProductCategoryPojo;
import org.monostudio.jpa.entities.ProductCategory;
import org.monostudio.jpa.repositories.ProductsCategoriesRepository;
import org.monostudio.jpa.services.conversion.ImagesConverterService;
import org.monostudio.jpa.services.CategoryEnrichmentService;
import org.monostudio.config.cache.CacheNames;

import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Public category browsing endpoints for customers.
 * No authentication required. All data is read-only.
 * Supports hierarchical tree navigation for the Collections UI.
 */
@RestController
@RequestMapping("/api/public/categories")
@Tag(name = "Public Category Data")
public class PublicCategoriesController {

    private final ProductsCategoriesRepository categoriesRepository;
    private final CategoryEnrichmentService categoryEnrichmentService;
    private final ImagesConverterService imagesConverterService;

    @Autowired
    public PublicCategoriesController(
        ProductsCategoriesRepository categoriesRepository,
        CategoryEnrichmentService categoryEnrichmentService,
        ImagesConverterService imagesConverterService
    ) {
        this.categoriesRepository = categoriesRepository;
        this.categoryEnrichmentService = categoryEnrichmentService;
        this.imagesConverterService = imagesConverterService;
    }

    /**
     * Returns the full category tree — root categories at the top,
     * all descendants nested under their parents as CategoryTreePojo nodes.
     * Each node is enriched with productCount.
     */
    @GetMapping
    @Operation(summary = "Get full public category tree")
    @Cacheable(cacheNames = CacheNames.PUBLIC_CATEGORY_TREE, key = "'tree'")
    public List<CategoryTreePojo> getCategoryTree() {
        List<ProductCategory> allCategories = categoriesRepository.findAll();

        // O(1) index: parent id -> children
        Map<Long, List<ProductCategory>> childrenByParentId = allCategories.stream()
            .filter(c -> c.getParent() != null)
            .collect(Collectors.groupingBy(c -> c.getParent().getId()));

        // Build tree starting from root nodes (parent == null)
        return allCategories.stream()
            .filter(c -> c.getParent() == null)
            .sorted(categoryDisplayOrderComparator())
            .map(root -> toTreeNode(root, childrenByParentId))
            .collect(Collectors.toList());
    }

    /**
     * Returns a single category node (by code) with its immediate children
     * and the productCount enriched on every node in the subtree.
     * Returns 404 if the code does not exist.
     */
    @GetMapping("/{code}")
    @Operation(summary = "Get a single category by code with its subtree")
    @Cacheable(cacheNames = CacheNames.PUBLIC_CATEGORY_BY_CODE, key = "#code")
    public CategoryTreePojo getCategoryByCode(@PathVariable String code) {
        ProductCategory root = categoriesRepository.findByCode(code)
            .orElseThrow(() -> new EntityNotFoundException(
                "Category not found: " + code));

        List<ProductCategory> allCategories = categoriesRepository.findAll();

        Map<Long, List<ProductCategory>> childrenByParentId = allCategories.stream()
            .filter(c -> c.getParent() != null)
            .collect(Collectors.groupingBy(c -> c.getParent().getId()));

        return toTreeNode(root, childrenByParentId);
    }

    private CategoryTreePojo toTreeNode(
            ProductCategory entity,
            Map<Long, List<ProductCategory>> childrenByParentId
    ) {
        // Build minimal parent sub-object inline (avoids self-reference recursion)
        ProductCategoryPojo parentPojo = null;
        if (entity.getParent() != null) {
            parentPojo = ProductCategoryPojo.builder()
                .id(entity.getParent().getId())
                .code(entity.getParent().getCode())
                .name(entity.getParent().getName())
                .imageUrl(entity.getParent().getImage() != null ? entity.getParent().getImage().getUrl() : null)
                .image(entity.getParent().getImage() != null ? imagesConverterService.convertToPojo(entity.getParent().getImage()) : null)
                .build();
        }

        CategoryTreePojo node = CategoryTreePojo.builder()
            .id(entity.getId())
            .code(entity.getCode())
            .name(entity.getName())
            .displayOrder(entity.getDisplayOrder())
            .parent(parentPojo)
            .imageUrl(entity.getImage() != null ? entity.getImage().getUrl() : null)
            .image(entity.getImage() != null ? imagesConverterService.convertToPojo(entity.getImage()) : null)
            .children(new ArrayList<>())
            .build();

        // Enrich with product count
        categoryEnrichmentService.enrichWithProductCount(node);

        // Recurse into children
        List<ProductCategory> children = childrenByParentId.getOrDefault(entity.getId(), List.of()).stream()
            .sorted(categoryDisplayOrderComparator())
            .toList();
        for (ProductCategory child : children) {
            node.getChildren().add(toTreeNode(child, childrenByParentId));
        }

        return node;
    }

    private static Comparator<ProductCategory> categoryDisplayOrderComparator() {
        return Comparator
            .comparing(ProductCategory::getDisplayOrder, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(ProductCategory::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
    }
}