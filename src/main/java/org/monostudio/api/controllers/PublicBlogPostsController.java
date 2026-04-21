package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.monostudio.jpa.services.crud.BlogPostsCrudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.monostudio.api.models.BlogPostPojo;
import org.monostudio.api.models.DataPagePojo;
import org.monostudio.api.services.PaginationService;
import org.monostudio.jpa.entities.BlogPostStatus;
import org.monostudio.jpa.repositories.BlogPostsRepository;
import org.monostudio.jpa.services.conversion.BlogPostsConverterService;
import org.monostudio.jpa.services.predicates.BlogPostsPredicateService;

import jakarta.persistence.EntityNotFoundException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public/blog")
@Tag(name = "Blog")
public class PublicBlogPostsController {

    private final BlogPostsRepository blogPostsRepository;
    private final BlogPostsConverterService converterService;
    private final BlogPostsCrudService crudService;
    private final BlogPostsPredicateService predicateService;
    private final PaginationService paginationService;

    @Autowired
    public PublicBlogPostsController(
        BlogPostsRepository blogPostsRepository,
        BlogPostsConverterService converterService,
        BlogPostsCrudService crudService,
        BlogPostsPredicateService predicateService,
        PaginationService paginationService
    ) {
        this.blogPostsRepository = blogPostsRepository;
        this.converterService = converterService;
        this.crudService = crudService;
        this.predicateService = predicateService;
        this.paginationService = paginationService;
    }

    @GetMapping
    @Operation(summary = "List published blog posts.")
    public DataPagePojo<BlogPostPojo> getPublishedPosts(@RequestParam Map<String, String> allRequestParams) {
        Map<String, String> filters = new HashMap<>(allRequestParams);
        filters.put("status", BlogPostStatus.PUBLISHED.name());

        int pageIndex = paginationService.determineRequestedPageIndex(allRequestParams);
        int pageSize = paginationService.determineRequestedPageSize(allRequestParams);

        return crudService.readMany(
            pageIndex,
            pageSize,
            null, // No specific sort for public yet
            predicateService.parseMap(filters)
        );
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get a blog post by slug.")
    public BlogPostPojo getPostBySlug(@PathVariable String slug) {
        return blogPostsRepository.findBySlug(slug)
            .filter(post -> post.getStatus() == BlogPostStatus.PUBLISHED)
            .map(converterService::convertToPojo)
            .orElseThrow(() -> new EntityNotFoundException("Blog post not found or not published"));
    }
}
