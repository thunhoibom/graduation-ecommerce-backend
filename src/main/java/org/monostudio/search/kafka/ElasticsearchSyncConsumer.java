package org.monostudio.search.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.monostudio.jpa.entities.BlogPost;
import org.monostudio.jpa.entities.Product;
import org.monostudio.jpa.entities.ProductVariant;
import org.monostudio.jpa.repositories.BlogPostsRepository;
import org.monostudio.jpa.repositories.OrderDetailsRepository;
import org.monostudio.jpa.repositories.ProductImagesRepository;
import org.monostudio.api.services.ProductPricingSnapshotService;
import org.monostudio.jpa.repositories.ProductsRepository;
import org.monostudio.jpa.repositories.UserBehaviorEventsRepository;
import org.monostudio.jpa.services.conversion.ProductsConverterService;
import org.monostudio.search.models.BlogPostDocument;
import org.monostudio.search.models.ProductDocument;
import org.monostudio.search.repositories.BlogPostSearchRepository;
import org.monostudio.search.repositories.ProductSearchRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

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
    private final OrderDetailsRepository orderDetailsRepository;
    private final UserBehaviorEventsRepository userBehaviorEventsRepository;
    private final ProductPricingSnapshotService productPricingSnapshotService;

    @KafkaListener(topics = "search-indexing-topic", groupId = "search-sync-group")
    @Transactional(readOnly = true)
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
                WeatherProfile weatherProfile = inferWeatherProfile(product);
                var pricing = productPricingSnapshotService.calculate(product);
                ProductDocument doc = ProductDocument.builder()
                        .id(product.getId().toString())
                        .productNumericId(product.getId())
                        .name(product.getName())
                        .barcode(product.getBarcode())
                        .description(product.getDescription())
                        .price(pricing.originalPrice())
                        .originalPrice(pricing.originalPrice())
                        .currentPrice(pricing.currentPrice())
                        .discountPercent(pricing.discountPercent())
                        .hasDiscount(pricing.hasDiscount())
                        .stockCurrent(product.getStockCurrent())
                        .variantColors(extractVariantTokens(product.getVariants(), ProductVariant::getColor))
                        .variantSizes(extractVariantTokens(product.getVariants(), ProductVariant::getSize))
                        .categoryName(product.getProductCategory() != null ? product.getProductCategory().getName() : null)
                        .categoryCodes(extractCategoryHierarchy(product.getProductCategory()))
                        .weatherTags(weatherProfile.tags())
                        .tempMin(weatherProfile.tempMin())
                        .tempMax(weatherProfile.tempMax())
                        .status(product.getStatus() != null ? product.getStatus().name() : null)
                        .unitsSold(clampInt(orderDetailsRepository.sumPaidFulfilledUnitsForProduct(product.getId())))
                        .viewCount(clampInt(userBehaviorEventsRepository.countProductViewsForProduct(product.getId())))
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

    private List<String> extractVariantTokens(
            List<ProductVariant> variants,
            Function<ProductVariant, String> getter
    ) {
        if (variants == null || variants.isEmpty()) {
            return List.of();
        }
        return variants.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private List<String> extractCategoryHierarchy(org.monostudio.jpa.entities.ProductCategory category) {
        List<String> codes = new ArrayList<>();
        org.monostudio.jpa.entities.ProductCategory current = category;
        while (current != null) {
            codes.add(current.getCode());
            current = current.getParent();
        }
        return codes;
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

    private WeatherProfile inferWeatherProfile(Product product) {
        String raw = ((product.getName() == null ? "" : product.getName()) + " "
                + (product.getDescription() == null ? "" : product.getDescription()) + " "
                + (product.getProductCategory() != null && product.getProductCategory().getName() != null
                ? product.getProductCategory().getName()
                : ""))
                .toLowerCase(Locale.ROOT);

        List<String> tags = new ArrayList<>();
        int tempMin = 20;
        int tempMax = 35;

        if (containsAny(raw, "ao khoac", "hoodie", "sweater", "len", "dai tay", "jacket", "coat")) {
            tempMin = 10;
            tempMax = 24;
            tags.add("cold");
            tags.add("windy");
        } else if (containsAny(raw, "ao mua", "raincoat", "chong nuoc", "du", "waterproof")) {
            tempMin = 16;
            tempMax = 30;
            tags.add("rain");
            tags.add("windy");
        } else if (containsAny(raw, "ao thun", "t-shirt", "tank", "short", "vay ngan")) {
            tempMin = 24;
            tempMax = 38;
            tags.add("clear");
            tags.add("hot");
        } else {
            tags.add("clear");
            tags.add("cloudy");
        }

        return new WeatherProfile(tags, tempMin, tempMax);
    }

    private boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private record WeatherProfile(List<String> tags, int tempMin, int tempMax) {
    }

    private static int clampInt(long value) {
        if (value <= 0) {
            return 0;
        }
        return (int) Math.min(value, Integer.MAX_VALUE);
    }
}
