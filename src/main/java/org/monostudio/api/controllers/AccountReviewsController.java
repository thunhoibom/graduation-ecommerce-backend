package org.monostudio.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.monostudio.api.models.ProductReviewPojo;
import org.monostudio.jpa.entities.Customer;
import org.monostudio.jpa.entities.User;
import org.monostudio.jpa.repositories.CustomersRepository;
import org.monostudio.jpa.repositories.UsersRepository;
import org.monostudio.jpa.services.crud.ProductReviewsCrudService;
import org.monostudio.common.exceptions.BadInputException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;

/**
 * Customer-authenticated endpoints for submitting and managing their own product reviews.
 */
@RestController
@RequestMapping("/api/account/reviews")
@Tag(name = "My Reviews")
@PreAuthorize("isAuthenticated()")
public class AccountReviewsController {

    private final ProductReviewsCrudService productReviewsCrudService;
    private final UsersRepository usersRepository;
    private final CustomersRepository customersRepository;

    @Autowired
    public AccountReviewsController(
        ProductReviewsCrudService productReviewsCrudService,
        UsersRepository usersRepository,
        CustomersRepository customersRepository
    ) {
        this.productReviewsCrudService = productReviewsCrudService;
        this.usersRepository = usersRepository;
        this.customersRepository = customersRepository;
    }

    /**
     * Submit a new product review.
     * The review starts in pending/visible=false state and must be approved by an admin.
     * verifiedPurchase is automatically set based on whether the customer has ordered the product.
     */
    @PostMapping
    @Operation(summary = "Submit a product review")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductReviewPojo submitReview(
        @Valid @RequestBody ProductReviewPojo input,
        Principal principal
    ) throws BadInputException {
        Long customerId = resolveCustomerId(principal);
        return productReviewsCrudService.createReview(input, customerId);
    }

    /**
     * List all reviews written by the authenticated customer (including pending ones).
     */
    @GetMapping
    @Operation(summary = "List all reviews written by the authenticated customer")
    public List<ProductReviewPojo> listMyReviews(Principal principal) {
        Long customerId = resolveCustomerId(principal);
        return productReviewsCrudService.readByCustomer(customerId);
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
}
