package org.monostudio.jpa.services.conversion.impl;

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.monostudio.api.models.ProductReviewPojo;
import org.monostudio.api.models.ProductReviewReplyPojo;
import org.monostudio.common.exceptions.BadInputException;
import org.monostudio.jpa.entities.Customer;
import org.monostudio.jpa.entities.Image;
import org.monostudio.jpa.entities.Person;
import org.monostudio.jpa.entities.Product;
import org.monostudio.jpa.entities.ProductReview;
import org.monostudio.jpa.entities.ReviewImage;
import org.monostudio.jpa.repositories.CustomersRepository;
import org.monostudio.jpa.repositories.ImagesRepository;
import org.monostudio.jpa.repositories.PeopleRepository;
import org.monostudio.jpa.repositories.ProductsRepository;
import org.monostudio.jpa.services.conversion.ProductReviewsConverterService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@NoArgsConstructor
public class ProductReviewsConverterServiceImpl
    implements ProductReviewsConverterService {

    private ProductsRepository productsRepository;
    private CustomersRepository customersRepository;
    private PeopleRepository peopleRepository;
    private ImagesRepository imagesRepository;

    @Autowired
    public ProductReviewsConverterServiceImpl(
        ProductsRepository productsRepository,
        CustomersRepository customersRepository,
        PeopleRepository peopleRepository,
        ImagesRepository imagesRepository
    ) {
        this.productsRepository = productsRepository;
        this.customersRepository = customersRepository;
        this.peopleRepository = peopleRepository;
        this.imagesRepository = imagesRepository;
    }

    @Override
    public ProductReviewPojo convertToPojo(ProductReview source) {
        if (source == null) return null;

        System.out.println("[DEBUG] Converting Review Entity to Pojo. ID: " + source.getId() + ", Images: " + (source.getImages() != null ? source.getImages().size() : "null"));

        ProductReviewPojo target = ProductReviewPojo.builder()
            .id(source.getId())
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

        // Map images
        if (source.getImages() != null) {
            target.setImageUrls(source.getImages().stream()
                .map(ri -> ri.getImage().getUrl())
                .collect(Collectors.toList()));
        }

        // Map replies
        if (source.getReplies() != null) {
            target.setReplies(source.getReplies().stream()
                .map(r -> ProductReviewReplyPojo.builder()
                    .id(r.getId())
                    .body(r.getBody())
                    .authorName(resolveAuthorName(r))
                    .isStaff(r.getUser() != null)
                    .createdAt(r.getCreatedAt())
                    .build())
                .collect(Collectors.toList()));
        }

        return target;
    }

    private String resolveAuthorName(org.monostudio.jpa.entities.ProductReviewReply reply) {
        if (reply.getUser() != null) {
            Person p = reply.getUser().getPerson();
            return p != null ? (p.getFirstName() + " " + p.getLastName()).trim() : "Staff";
        }
        if (reply.getCustomer() != null && reply.getCustomer().getPerson() != null) {
            Person p = reply.getCustomer().getPerson();
            return (p.getFirstName() + " " + p.getLastName()).trim();
        }
        return "Người dùng ẩn danh";
    }

    @Override
    public ProductReview convertToNewEntity(ProductReviewPojo source) throws BadInputException {
        ProductReview target = ProductReview.builder()
            .rating(source.getRating())
            .title(source.getTitle())
            .body(source.getBody())
            .approved(true) // For testing: automatically approve new reviews. Revert to false for moderation.
            .verifiedPurchase(false) // set by caller if purchase verified
            .build();

        if (!StringUtils.isBlank(source.getProductBarcode())) {
            Product product = productsRepository.findByBarcode(source.getProductBarcode())
                .orElseThrow(() -> new BadInputException("Product not found with barcode: " + source.getProductBarcode()));
            target.setProduct(product);
        } else {
            throw new BadInputException("Product barcode is required");
        }

        // Map images if IDs provided
        System.out.println("[DEBUG] Converting Review Pojo. Image IDs: " + source.getImageIds());
        if (source.getImageIds() != null && !source.getImageIds().isEmpty()) {
            List<ReviewImage> reviewImages = new ArrayList<>();
            for (Long imageId : source.getImageIds()) {
                if (imageId == null) continue; 
                Image img = imagesRepository.findById(imageId)
                    .orElseThrow(() -> new BadInputException("Image not found: " + imageId));
                reviewImages.add(ReviewImage.builder()
                    .review(target)
                    .image(img)
                    .build());
            }
            target.setImages(reviewImages);
        }

        return target;
    }

    @Override
    public ProductReview applyChangesToExistingEntity(ProductReviewPojo source, ProductReview target) {
        throw new UnsupportedOperationException("This method is deprecated");
    }
}
