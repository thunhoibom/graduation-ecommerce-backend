package org.monostudio.jpa.services.conversion.impl;

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.ProductReviewPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Customer;
import org.monostudio.jpa.entities.Person;
import org.monostudio.jpa.entities.Product;
import org.monostudio.jpa.entities.ProductReview;
import org.monostudio.jpa.repositories.CustomersRepository;
import org.monostudio.jpa.repositories.PeopleRepository;
import org.monostudio.jpa.repositories.ProductsRepository;
import org.monostudio.jpa.services.conversion.ProductReviewsConverterService;

@Service
@NoArgsConstructor
public class ProductReviewsConverterServiceImpl
    implements ProductReviewsConverterService {

    private ProductsRepository productsRepository;
    private CustomersRepository customersRepository;
    private PeopleRepository peopleRepository;

    @Autowired
    public ProductReviewsConverterServiceImpl(
        ProductsRepository productsRepository,
        CustomersRepository customersRepository,
        PeopleRepository peopleRepository
    ) {
        this.productsRepository = productsRepository;
        this.customersRepository = customersRepository;
        this.peopleRepository = peopleRepository;
    }

    @Override
    public ProductReviewPojo convertToPojo(ProductReview source) {
        ProductReviewPojo target = ProductReviewPojo.builder()
            .rating(source.getRating())
            .title(source.getTitle())
            .body(source.getBody())
            .approved(source.isApproved())
            .verifiedPurchase(source.isVerifiedPurchase())
            .createdAt(source.getCreatedAt())
            .updatedAt(source.getUpdatedAt())
            .build();

        Product product = source.getProduct();
        if (product != null) {
            target.setProductBarcode(product.getBarcode());
            target.setProductName(product.getName());
        }

        Customer customer = source.getCustomer();
        if (customer != null) {
            Person person = customer.getPerson();
            if (person != null) {
                String reviewerName = person.getFirstName() + " " + person.getLastName();
                target.setReviewerName(reviewerName.trim());
            }
        }

        return target;
    }

    @Override
    public ProductReview convertToNewEntity(ProductReviewPojo source) throws BadInputException {
        ProductReview target = ProductReview.builder()
            .rating(source.getRating())
            .title(source.getTitle())
            .body(source.getBody())
            .approved(false) // reviews must be approved by admin
            .verifiedPurchase(false) // set by caller if purchase verified
            .build();

        if (!StringUtils.isBlank(source.getProductBarcode())) {
            Product product = productsRepository.findByBarcode(source.getProductBarcode())
                .orElseThrow(() -> new BadInputException("Product not found with barcode: " + source.getProductBarcode()));
            target.setProduct(product);
        } else {
            throw new BadInputException("Product barcode is required");
        }

        return target;
    }

    @Override
    public ProductReview applyChangesToExistingEntity(ProductReviewPojo source, ProductReview target) {
        throw new UnsupportedOperationException("This method is deprecated");
    }
}
