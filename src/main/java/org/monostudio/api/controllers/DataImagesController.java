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
import org.monostudio.api.models.ImagePojo;
import org.monostudio.api.services.PaginationService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Image;
import org.monostudio.jpa.services.SortSpecParserService;
import org.monostudio.jpa.services.crud.ImagesCrudService;
import org.monostudio.jpa.services.predicates.ImagesPredicateService;
import org.monostudio.jpa.sortspecs.ImagesSortSpec;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.Map;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/api/data/images")
@Tag(name = "Images management")
@PreAuthorize("isAuthenticated()")
public class DataImagesController
    extends DataCrudGenericController<ImagePojo, Image> {

    @Autowired
    public DataImagesController(
        PaginationService paginationService,
        SortSpecParserService sortService,
        ImagesCrudService crudService,
        ImagesPredicateService predicateService
    ) {
        super(paginationService, sortService, crudService, predicateService);
    }

    @Override
    @GetMapping
    @Operation(summary = "List image links data.")
    @PreAuthorize("hasAuthority('images:read')")
    public DataPagePojo<ImagePojo> readMany(@RequestParam Map<String, String> allRequestParams) {
        return super.readMany(allRequestParams);
    }

    @Override
    @PostMapping
    @Operation(summary = "Define new image links.")
    @ResponseStatus(CREATED)
    @PreAuthorize("hasAuthority('images:create')")
    public void create( ImagePojo input)
        throws BadInputException, EntityExistsException {
        crudService.create(input);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace image links data.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('images:update')")
    public void update(ImagePojo input, @PathVariable Long id)
        throws EntityNotFoundException, BadInputException {
        crudService.update(input, id);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update parts of image links data.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('images:update')")
    public void partialUpdate(
        @RequestBody Map<String, Object> input,
        @PathVariable Long id
    ) throws BadInputException, EntityNotFoundException {
        crudService.partialUpdate(input, id)
            .orElseThrow(() -> new EntityNotFoundException("No element was found to update"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove image links.")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('images:delete')")
    public void delete(@PathVariable Long id)
        throws EntityNotFoundException {
        crudService.delete(id);
    }

    @Override
    protected Map<String, OrderSpecifier<?>> getOrderSpecMap() {
        return ImagesSortSpec.ORDER_SPEC_MAP;
    }
}
