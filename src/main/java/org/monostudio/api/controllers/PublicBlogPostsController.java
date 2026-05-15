package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.monostudio.jpa.entities.BlogPost;
import org.monostudio.jpa.services.crud.BlogPostsCrudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.monostudio.api.models.BlogPostPojo;
import org.monostudio.api.models.DataPagePojo;
import org.monostudio.api.services.PaginationService;
import org.monostudio.jpa.entities.BlogPostStatus;
import org.monostudio.jpa.repositories.BlogPostsRepository;
import org.monostudio.jpa.services.SortSpecParserService;
import org.monostudio.jpa.services.conversion.BlogPostsConverterService;
import org.monostudio.jpa.services.predicates.BlogPostsPredicateService;
import org.monostudio.jpa.sortspecs.BlogPostsSortSpec;
import org.monostudio.search.models.BlogPostDocument;
import org.monostudio.search.services.SearchService;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public/blog")
@Tag(name = "Blog")
public class PublicBlogPostsController {

    private final BlogPostsRepository blogPostsRepository;
    private final BlogPostsConverterService converterService;
    private final BlogPostsCrudService crudService;
    private final BlogPostsPredicateService predicateService;
    private final PaginationService paginationService;
    private final SortSpecParserService sortService;
    private final SearchService searchService;

    @Autowired
    public PublicBlogPostsController(
        BlogPostsRepository blogPostsRepository,
        BlogPostsConverterService converterService,
        BlogPostsCrudService crudService,
        BlogPostsPredicateService predicateService,
        PaginationService paginationService,
        SortSpecParserService sortService,
        SearchService searchService
    ) {
        this.blogPostsRepository = blogPostsRepository;
        this.converterService = converterService;
        this.crudService = crudService;
        this.predicateService = predicateService;
        this.paginationService = paginationService;
        this.sortService = sortService;
        this.searchService = searchService;
    }

    @GetMapping
    @Operation(summary = "List published blog posts.")
    public DataPagePojo<BlogPostPojo> getPublishedPosts(@RequestParam Map<String, String> allRequestParams) {
        Map<String, String> filters = new HashMap<>(allRequestParams);
        filters.put("status", BlogPostStatus.PUBLISHED.name());
        filters.put("publishedOnly", "true");

        int pageIndex = paginationService.determineRequestedPageIndex(allRequestParams);
        int pageSize = paginationService.determineRequestedPageSize(allRequestParams);
        if (!filters.containsKey("sortBy")) {
            filters.put("sortBy", "publishedAt");
            filters.put("order", "desc");
        }

        return crudService.readMany(
            pageIndex,
            pageSize,
            sortService.parse(BlogPostsSortSpec.ORDER_SPEC_MAP, filters),
            predicateService.parseMap(filters)
        );
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get a blog post by slug.")
    public BlogPostPojo getPostBySlug(@PathVariable String slug) {
        return blogPostsRepository.findBySlug(slug)
            .filter(post -> post.getStatus() == BlogPostStatus.PUBLISHED
                && post.getPublishedAt() != null
                && !post.getPublishedAt().isAfter(LocalDateTime.now()))
            .map(converterService::convertToPojo)
            .orElseThrow(() -> new EntityNotFoundException("Blog post not found or not published"));
    }

    @GetMapping("/search")
    @Operation(summary = "Search published blog posts.")
    public DataPagePojo<BlogPostDocument> searchPosts(@RequestParam Map<String, String> params) {
        int pageIndex = paginationService.determineRequestedPageIndex(params);
        int pageSize = paginationService.determineRequestedPageSize(params);
        String query = params.getOrDefault("q", "");
        List<String> excludeIds = parseExcludeIds(params.get("excludeIds"));

        return searchService.searchBlogPosts(
            query,
            BlogPostStatus.PUBLISHED.name(),
            pageIndex,
            pageSize,
            excludeIds
        );
    }

    @GetMapping("/{slug}/related")
    @Operation(summary = "Get related posts by slug.")
    public List<BlogPostPojo> getRelatedPosts(
        @PathVariable String slug,
        @RequestParam(defaultValue = "6") int limit
    ) {
        BlogPost current = blogPostsRepository.findBySlug(slug)
            .orElseThrow(() -> new EntityNotFoundException("Blog post not found"));

        if (current.getStatus() != BlogPostStatus.PUBLISHED
            || current.getPublishedAt() == null
            || current.getPublishedAt().isAfter(LocalDateTime.now())) {
            throw new EntityNotFoundException("Blog post not found");
        }

        int safeLimit = Math.min(Math.max(limit, 1), 12);
        List<BlogPostPojo> bySearch = searchService.searchRelatedBlogPosts(current, safeLimit)
            .stream()
            .map(converterService::convertToPojo)
            .toList();
        if (!bySearch.isEmpty()) {
            return bySearch;
        }

        return blogPostsRepository
            .findTop6ByStatusAndPublishedAtLessThanEqualAndIdNotOrderByPublishedAtDesc(
                BlogPostStatus.PUBLISHED, LocalDateTime.now(), current.getId())
            .stream()
            .limit(safeLimit)
            .map(converterService::convertToPojo)
            .collect(Collectors.toList());
    }

    private static List<String> parseExcludeIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String[] parts = raw.split(",");
        List<String> out = new ArrayList<>();
        for (String part : parts) {
            if (part != null) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    out.add(trimmed);
                }
            }
            if (out.size() >= 100) {
                break;
            }
        }
        return out;
    }
}
