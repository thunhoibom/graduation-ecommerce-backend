package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.monostudio.api.DataCrudGenericController;
import org.monostudio.api.models.DataPagePojo;
import org.monostudio.api.models.ProductReviewPojo;
import org.monostudio.api.services.PaginationService;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Customer;
import org.monostudio.jpa.entities.ProductReview;
import org.monostudio.jpa.entities.User;
import org.monostudio.jpa.repositories.CustomersRepository;
import org.monostudio.jpa.repositories.UsersRepository;
import org.monostudio.jpa.services.SortSpecParserService;
import org.monostudio.jpa.services.crud.ProductReviewsCrudService;
import org.monostudio.jpa.services.predicates.ProductReviewsPredicateService;
import org.monostudio.jpa.sortspecs.ProductReviewsSortSpec;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;

import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequestMapping("/data/product-reviews")
@Tag(name = "Product Reviews — Admin")
@PreAuthorize("hasAuthority('productReviews:read')")
public class DataProductReviewsController
    extends DataCrudGenericController<ProductReviewPojo, ProductReview> {

    private final ProductReviewsCrudService productReviewsCrudService;
    private final UsersRepository usersRepository;
    private final CustomersRepository customersRepository;

    @Autowired
    public DataProductReviewsController(
        PaginationService paginationService,
        SortSpecParserService sortSpecParserService,
        ProductReviewsCrudService crudService,
        ProductReviewsPredicateService predicateService,
        UsersRepository usersRepository,
        CustomersRepository customersRepository
    ) {
        super(paginationService, sortSpecParserService, crudService, predicateService);
        this.productReviewsCrudService = crudService;
        this.usersRepository = usersRepository;
        this.customersRepository = customersRepository;
    }

    @Override
    @GetMapping
    @Operation(summary = "List all product reviews (admin — includes unapproved)")
    public DataPagePojo<ProductReviewPojo> readMany(@RequestParam Map<String, String> allRequestParams) {
        return super.readMany(allRequestParams);
    }

    @Override
    @PostMapping
    @Operation(summary = "Create a product review on behalf of a customer (admin — bypasses approval)")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('productReviews:create')")
    public void create(
        @Valid @RequestBody ProductReviewPojo input,
        @RequestParam Long customerId
    ) throws BadInputException, EntityExistsException {
        crudService.createReview(input, customerId);
    }

    /**
     * Approve a pending review.
     */
    @PatchMapping("/{id}/approve")
    @Operation(summary = "Approve a product review")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('productReviews:update')")
    public void approve(@PathVariable Long id) throws EntityNotFoundException {
        productReviewsCrudService.setApproval(id, true);
    }

    /**
     * Reject a pending review.
     */
    @PatchMapping("/{id}/reject")
    @Operation(summary = "Reject a product review")
    @ResponseStatus(NO_CONTENT)
    @PreAuthorize("hasAuthority('productReviews:update')")
    public void reject(@PathVariable Long id) throws EntityNotFoundException {
        productReviewsCrudService.setApproval(id, false);
    }

    private Long resolveCustomerId(Principal principal) {
        String username = principal.getName();
        User user = usersRepository.findByNameWithProfile(username)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (user.getPerson() == null) {
            throw new EntityNotFoundException("User does not have a profile");
        }
        Customer customer = customersRepository.findByPersonId(user.getPerson().getId())
            .orElseThrow(() -> new EntityNotFoundException("No customer profile found for user"));
        return customer.getId();
    }

    @Override
    protected Map<String, com.querydsl.core.types.OrderSpecifier<?>> getOrderSpecMap() {
        return ProductReviewsSortSpec.ORDER_SPEC_MAP;
    }
}
