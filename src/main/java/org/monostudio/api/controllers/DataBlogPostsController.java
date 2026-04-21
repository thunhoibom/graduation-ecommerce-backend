package org.monostudio.api.controllers;

import com.querydsl.core.types.OrderSpecifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.monostudio.api.DataCrudGenericController;
import org.monostudio.api.models.BlogPostPojo;
import org.monostudio.api.models.DataPagePojo;
import org.monostudio.api.services.PaginationService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.BlogPost;
import org.monostudio.jpa.services.SortSpecParserService;
import org.monostudio.jpa.services.crud.BlogPostsCrudService;
import org.monostudio.jpa.services.predicates.BlogPostsPredicateService;
import org.monostudio.jpa.sortspecs.BlogPostsSortSpec;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.util.Map;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/data/blog-posts")
@Tag(name = "Blog Management")
@PreAuthorize("isAuthenticated()")
public class DataBlogPostsController
    extends DataCrudGenericController<BlogPostPojo, BlogPost> {

    @Autowired
    public DataBlogPostsController(
        PaginationService paginationService,
        SortSpecParserService sortService,
        BlogPostsCrudService crudService,
        BlogPostsPredicateService predicateService
    ) {
        super(paginationService, sortService, crudService, predicateService);
    }

    @Override
    @GetMapping
    @Operation(summary = "List blog posts.")
    @PreAuthorize("hasAuthority('blog:read')")
    public DataPagePojo<BlogPostPojo> readMany(@RequestParam Map<String, String> allRequestParams) {
        return super.readMany(allRequestParams);
    }

    @Override
    @PostMapping
    @Operation(summary = "Create a new blog post.")
    @ResponseStatus(CREATED)
    @PreAuthorize("hasAuthority('blog:create')")
    public void create(@RequestBody BlogPostPojo input)
        throws BadInputException, EntityExistsException {
        crudService.create(input);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update blog post data.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('blog:update')")
    public void update(@RequestBody BlogPostPojo input, @PathVariable Long id)
        throws BadInputException, EntityNotFoundException {
        crudService.update(input, id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove blog post.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('blog:delete')")
    public void delete(@PathVariable Long id)
        throws EntityNotFoundException {
        crudService.delete(id);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Partial update of blog post.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('blog:update')")
    public void partialUpdate(
        @RequestBody Map<String, Object> input,
        @PathVariable Long id
    ) throws BadInputException, EntityNotFoundException {
        crudService.partialUpdate(input, id)
            .orElseThrow(() -> new EntityNotFoundException("Blog post not found: " + id));
    }

    @Override
    protected Map<String, OrderSpecifier<?>> getOrderSpecMap() {
        return BlogPostsSortSpec.ORDER_SPEC_MAP;
    }
}
