package org.monostudio.search.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.monostudio.jpa.entities.BlogPost;
import org.monostudio.jpa.entities.Product;
import org.monostudio.jpa.repositories.BlogPostsRepository;
import org.monostudio.jpa.repositories.ProductImagesRepository;
import org.monostudio.jpa.repositories.ProductsRepository;
import org.monostudio.jpa.services.conversion.ProductsConverterService;
import org.monostudio.search.models.BlogPostDocument;
import org.monostudio.search.models.ProductDocument;
import org.monostudio.search.repositories.BlogPostSearchRepository;
import org.monostudio.search.repositories.ProductSearchRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ElasticsearchSyncConsumer {

    private final ProductsRepository productsRepository;
    private final BlogPostsRepository blogPostsRepository;
    private final ProductSearchRepository productSearchRepository;
    private final BlogPostSearchRepository blogPostSearchRepository;
    private final ProductImagesRepository productImagesRepository;
    private final ProductsConverterService productsConverterService;

    @KafkaListener(topics = "search-indexing-topic", groupId = "search-sync-group")
    public void consumeIndexEvent(IndexEvent event) {
        log.info("Received index event from Kafka: {}", event);

        try {
            if ("DELETE".equals(event.getOperation())) {
                handleDelete(event);
            } else {
                handleUpsert(event);
            }
        } catch (Exception e) {
            log.error("Error processing index event: {}", event, e);
        }
    }

    private void handleUpsert(IndexEvent event) {
        if ("PRODUCT".equals(event.getEntityType())) {
            productsRepository.findById(event.getEntityId()).ifPresent(product -> {
                ProductDocument doc = ProductDocument.builder()
                        .id(product.getId().toString())
                        .name(product.getName())
                        .barcode(product.getBarcode())
                        .description(product.getDescription())
                        .price(product.getPrice())
                        .categoryName(product.getProductCategory() != null ? product.getProductCategory().getName() : null)
                        .status(product.getStatus() != null ? product.getStatus().name() : null)
                        .primaryImageUrl(productsConverterService.extractPrimaryImageUrl(
                                productImagesRepository.deepFindProductImagesByProductIdOrdered(product.getId())
                        ))
                        .build();
                productSearchRepository.save(doc);
                log.info("Indexed Product: {}", doc.getId());
            });
        } else if ("BLOG_POST".equals(event.getEntityType())) {
            blogPostsRepository.findById(event.getEntityId()).ifPresent(post -> {
                BlogPostDocument doc = BlogPostDocument.builder()
                        .id(post.getId().toString())
                        .title(post.getTitle())
                        .slug(post.getSlug())
                        .summary(post.getSummary())
                        .content(post.getContent())
                        .status(post.getStatus() != null ? post.getStatus().name() : null)
                        .build();
                blogPostSearchRepository.save(doc);
                log.info("Indexed BlogPost: {}", doc.getId());
            });
        }
    }

    private void handleDelete(IndexEvent event) {
        if ("PRODUCT".equals(event.getEntityType())) {
            productSearchRepository.deleteById(event.getEntityId().toString());
            log.info("Deleted Product from Index: {}", event.getEntityId());
        } else if ("BLOG_POST".equals(event.getEntityType())) {
            blogPostSearchRepository.deleteById(event.getEntityId().toString());
            log.info("Deleted BlogPost from Index: {}", event.getEntityId());
        }
    }
}
