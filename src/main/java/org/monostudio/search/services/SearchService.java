package org.monostudio.search.services;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.FieldValueFactorModifier;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScore;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.monostudio.api.models.DataPagePojo;
import org.monostudio.api.models.WeatherCategoryRecommendationPojo;
import org.monostudio.api.models.WeatherContextPojo;
import org.monostudio.jpa.entities.BlogPost;
import org.monostudio.api.services.ProductPricingSnapshotService;
import org.monostudio.jpa.repositories.BlogPostsRepository;
import org.monostudio.jpa.repositories.ProductsRepository;
import org.monostudio.search.models.ProductDocument;
import org.monostudio.search.models.BlogPostDocument;
import org.monostudio.search.repositories.ProductSearchRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import co.elastic.clients.json.JsonData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SearchService {

    private static final int MAX_EXCLUDE_IDS = 100;

    private final ProductSearchRepository productSearchRepository;
    private final BlogPostsRepository blogPostsRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final WeatherContextService weatherContextService;
    private final ProductsRepository productsRepository;
    private final ProductPricingSnapshotService productPricingSnapshotService;

    public DataPagePojo<ProductDocument> searchProducts(
            String keyword,
            Integer minPrice,
            Integer maxPrice,
            String category,
            String status,
            int pageIndex,
            int pageSize,
            Sort sort
    ) {
        return searchProducts(
                keyword,
                minPrice,
                maxPrice,
                category,
                status,
                pageIndex,
                pageSize,
                sort,
                null,
                null,
                null,
                null
        );
    }

    public DataPagePojo<ProductDocument> searchProducts(
            String keyword,
            Integer minPrice,
            Integer maxPrice,
            String category,
            String status,
            int pageIndex,
            int pageSize,
            Sort sort,
            List<String> excludeIds
    ) {
        return searchProducts(
                keyword,
                minPrice,
                maxPrice,
                category,
                status,
                pageIndex,
                pageSize,
                sort,
                excludeIds,
                null,
                null,
                null
        );
    }

    public DataPagePojo<ProductDocument> searchProducts(
            String keyword,
            Integer minPrice,
            Integer maxPrice,
            String category,
            String status,
            int pageIndex,
            int pageSize,
            Sort sort,
            List<String> excludeIds,
            Boolean inStockOnly,
            String color,
            String size
    ) {
        log.info(
                "[ES DEBUG] Searching with keyword: '{}', category: '{}', price: {}-{}, inStockOnly: {}, color: {}, size: {}",
                keyword,
                category,
                minPrice,
                maxPrice,
                inStockOnly,
                color,
                size
        );

        // Map JPA-style sort properties to Elasticsearch fields (name is analyzed text → sort on name.keyword)
        Sort esSort = toElasticsearchSort(sort);

        Pageable pageable = PageRequest.of(pageIndex, pageSize, esSort);

        String colorToken = normalizeFilterToken(color);
        String sizeToken = normalizeFilterToken(size);

        var queryBuilder = NativeQuery.builder()
                .withPageable(pageable)
                .withQuery(q -> q.functionScore(fs -> fs
                        .query(qb -> qb.bool(b -> {
                            if (keyword != null && !keyword.isBlank()) {
                                b.must(m -> m.match(mt -> mt
                                        .field("all")
                                        .query(keyword)
                                        .fuzziness("AUTO")
                                        .prefixLength(1)
                                        .maxExpansions(40)));
                            }

                            if (status != null) {
                                b.filter(f -> f.term(t -> t.field("status").value(status)));
                            }

                            if (category != null && !category.isBlank()) {
                                b.filter(f -> f.term(t -> t.field("categoryCodes").value(category)));
                            }

                            if (minPrice != null || maxPrice != null) {
                                b.filter(f -> f.range(r -> {
                                    if (minPrice != null) {
                                        r.gte(JsonData.of(minPrice));
                                    }
                                    if (maxPrice != null) {
                                        r.lte(JsonData.of(maxPrice));
                                    }
                                    return r.field("price");
                                }));
                            }

                            if (Boolean.TRUE.equals(inStockOnly)) {
                                b.filter(f -> f.range(r -> r.field("stockCurrent").gt(JsonData.of(0))));
                            }

                            if (colorToken != null) {
                                b.filter(f -> f.term(t -> t.field("variantColors").value(colorToken)));
                            }

                            if (sizeToken != null) {
                                b.filter(f -> f.term(t -> t.field("variantSizes").value(sizeToken)));
                            }

                            applyExcludeProductIds(b, excludeIds);

                            return b;
                        }))
                        .functions(
                                FunctionScore.of(f -> f.fieldValueFactor(fvf -> fvf
                                        .field("unitsSold")
                                        .factor(0.045)
                                        .modifier(FieldValueFactorModifier.Log1p)
                                        .missing(0.0))),
                                FunctionScore.of(f -> f.fieldValueFactor(fvf -> fvf
                                        .field("viewCount")
                                        .factor(0.03)
                                        .modifier(FieldValueFactorModifier.Log1p)
                                        .missing(0.0))),
                                FunctionScore.of(f -> f.fieldValueFactor(fvf -> fvf
                                        .field("productNumericId")
                                        .factor(0.000012)
                                        .modifier(FieldValueFactorModifier.None)
                                        .missing(0.0)))
                        )
                        .scoreMode(FunctionScoreMode.Sum)
                        .boostMode(FunctionBoostMode.Sum)
                ));

        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(queryBuilder.build(), ProductDocument.class);

        List<ProductDocument> items = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        enrichCatalogPricing(items);

        log.info("[ES DEBUG] Search finished. Hits: {}", searchHits.getTotalHits());

        return new DataPagePojo<>(items, pageIndex, searchHits.getTotalHits(), pageSize);
    }

    private static String normalizeFilterToken(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t.toLowerCase(Locale.ROOT);
    }

    /**
     * Personalized "for you" rail: optional full-text match on {@code all}, optional boost from category codes
     * derived from recent PRODUCT_VIEW events, and optional exclusion of product ids (e.g. current search page).
     * When keyword is blank, matches all published products with the same boosts/filters (e.g. homepage rail).
     */
    public DataPagePojo<ProductDocument> searchForYou(
            String keyword,
            List<String> excludeIds,
            List<String> categoryBoostCodes,
            String placement,
            String status,
            int pageIndex,
            int pageSize,
            Sort sort
    ) {
        Sort esSort = toElasticsearchSort(sort);

        Pageable pageable = PageRequest.of(pageIndex, pageSize, esSort);

        List<String> boosts = categoryBoostCodes == null ? List.of() : categoryBoostCodes;
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        String normalizedPlacement = placement == null ? "" : placement.trim().toLowerCase();
        float categoryBoostWeight = switch (normalizedPlacement) {
            case "cart", "checkout_success" -> 2.6f;
            case "pdp" -> 2.3f;
            default -> 2.0f;
        };

        var queryBuilder = NativeQuery.builder()
                .withPageable(pageable)
                .withQuery(q -> q.functionScore(fs -> fs
                        .query(qb -> qb.bool(b -> {
                            if (hasKeyword) {
                                b.must(m -> m.match(mt -> mt
                                        .field("all")
                                        .query(keyword)
                                        .fuzziness("AUTO")
                                        .prefixLength(1)
                                        .maxExpansions(40)));
                            }
                            if (status != null) {
                                b.filter(f -> f.term(t -> t.field("status").value(status)));
                            }
                            for (String cat : boosts) {
                                if (cat != null && !cat.isBlank()) {
                                    b.should(s -> s.term(t -> t.field("categoryCodes").value(cat).boost(categoryBoostWeight)));
                                }
                            }
                            b.minimumShouldMatch("0");
                            applyExcludeProductIds(b, excludeIds);
                            return b;
                        }))
                        .functions(
                                FunctionScore.of(f -> f.fieldValueFactor(fvf -> fvf
                                        .field("unitsSold")
                                        .factor(0.045)
                                        .modifier(FieldValueFactorModifier.Log1p)
                                        .missing(0.0))),
                                FunctionScore.of(f -> f.fieldValueFactor(fvf -> fvf
                                        .field("viewCount")
                                        .factor(0.03)
                                        .modifier(FieldValueFactorModifier.Log1p)
                                        .missing(0.0))),
                                FunctionScore.of(f -> f.fieldValueFactor(fvf -> fvf
                                        .field("productNumericId")
                                        .factor(0.000012)
                                        .modifier(FieldValueFactorModifier.None)
                                        .missing(0.0)))
                        )
                        .scoreMode(FunctionScoreMode.Sum)
                        .boostMode(FunctionBoostMode.Sum)
                ));

        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(queryBuilder.build(), ProductDocument.class);
        List<ProductDocument> items = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

        enrichCatalogPricing(items);

        log.info("[ES DEBUG] For-you search finished. Hits: {}", searchHits.getTotalHits());
        return new DataPagePojo<>(items, pageIndex, searchHits.getTotalHits(), pageSize);
    }

    /**
     * Converts Spring Data {@link Sort} from JPA/QueryDSL naming to Elasticsearch document fields.
     * Avoids {@code Sort.by(empty)} when the parser returns {@link Sort#unsorted()}.
     */
    private static Sort toElasticsearchSort(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return Sort.unsorted();
        }
        return Sort.by(sort.stream().map(order -> {
            String property = order.getProperty();
            if ("productCategory.name".equals(property)) {
                property = "categoryName";
            } else if ("name".equals(property)) {
                property = "name.keyword";
            } else if ("id".equals(property) || "createdAt".equals(property)) {
                property = "productNumericId";
            }
            return new Sort.Order(order.getDirection(), property);
        }).collect(Collectors.toList()));
    }

    private static void applyExcludeProductIds(co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder b, List<String> excludeIds) {
        if (excludeIds == null || excludeIds.isEmpty()) {
            return;
        }
        List<FieldValue> vals = excludeIds.stream()
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .limit(MAX_EXCLUDE_IDS)
                .map(FieldValue::of)
                .toList();
        if (vals.isEmpty()) {
            return;
        }
        b.mustNot(mn -> mn.terms(t -> t.field("id").terms(tv -> tv.value(vals))));
    }

    public List<ProductDocument> searchProductsSimple(String keyword) {
        return productSearchRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword);
    }

    public DataPagePojo<BlogPostDocument> searchBlogPosts(
        String keyword,
        String status,
        int pageIndex,
        int pageSize,
        List<String> excludeIds
    ) {
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        var queryBuilder = NativeQuery.builder()
            .withPageable(pageable)
            .withQuery(q -> q.bool(b -> {
                if (keyword != null && !keyword.isBlank()) {
                    b.must(m -> m.match(mt -> mt.field("all").query(keyword)));
                }
                if (status != null && !status.isBlank()) {
                    b.filter(f -> f.term(t -> t.field("status").value(status)));
                }
                if (excludeIds != null && !excludeIds.isEmpty()) {
                    List<FieldValue> vals = excludeIds.stream()
                        .filter(s -> s != null && !s.isBlank())
                        .distinct()
                        .limit(MAX_EXCLUDE_IDS)
                        .map(FieldValue::of)
                        .toList();
                    if (!vals.isEmpty()) {
                        b.mustNot(mn -> mn.terms(t -> t.field("id").terms(tv -> tv.value(vals))));
                    }
                }
                return b;
            }));

        SearchHits<BlogPostDocument> searchHits = elasticsearchOperations.search(queryBuilder.build(), BlogPostDocument.class);
        List<BlogPostDocument> items = searchHits.getSearchHits().stream()
            .map(SearchHit::getContent)
            .collect(Collectors.toList());
        return new DataPagePojo<>(items, pageIndex, searchHits.getTotalHits(), pageSize);
    }

    public List<BlogPost> searchRelatedBlogPosts(BlogPost currentPost, int limit) {
        if (currentPost == null || currentPost.getId() == null) {
            return List.of();
        }
        String queryText = String.join(" ",
            currentPost.getTitle() == null ? "" : currentPost.getTitle(),
            currentPost.getSummary() == null ? "" : currentPost.getSummary()
        ).trim();

        if (queryText.isBlank()) {
            return List.of();
        }

        DataPagePojo<BlogPostDocument> result = searchBlogPosts(
            queryText,
            "PUBLISHED",
            0,
            limit,
            List.of(currentPost.getId().toString())
        );
        if (result.getItems() == null || result.getItems().isEmpty()) {
            return List.of();
        }
        List<Long> ids = result.getItems().stream()
            .map(BlogPostDocument::getId)
            .filter(id -> id != null && !id.isBlank())
            .map(Long::valueOf)
            .toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        java.util.Map<Long, Integer> order = new java.util.HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            order.put(ids.get(i), i);
        }
        return blogPostsRepository.findAllById(ids).stream()
            .sorted(java.util.Comparator.comparingInt(post -> order.getOrDefault(post.getId(), Integer.MAX_VALUE)))
            .toList();
    }

    public WeatherCategoryRecommendationPojo recommendByWeatherAndCategory(
            String categoryCode,
            Double latitude,
            Double longitude,
            int limit
    ) {
        WeatherContextPojo weatherContext = weatherContextService.resolve(latitude, longitude);
        int targetTemp = weatherContext.getTemperature() != null
                ? (int) Math.round(weatherContext.getTemperature())
                : 25;

        Pageable pageable = PageRequest.of(0, Math.max(1, limit));
        Query query = NativeQuery.builder()
                .withPageable(pageable)
                .withQuery(q -> q.bool(b -> b
                        .filter(f -> f.term(t -> t.field("status").value("PUBLISHED")))
                        .filter(f -> f.term(t -> t.field("categoryCodes").value(categoryCode)))
                        .should(s -> s.term(t -> t
                                .field("weatherTags")
                                .value(weatherContext.getWeatherTag())
                                .boost(2.0f)))
                        .should(s -> s.bool(tempBoost -> tempBoost
                                .must(m1 -> m1.range(r -> r
                                        .field("tempMin")
                                        .lte(JsonData.of(targetTemp))))
                                .must(m2 -> m2.range(r -> r
                                        .field("tempMax")
                                        .gte(JsonData.of(targetTemp))))
                                .boost(1.5f)))
                        .minimumShouldMatch("0")
                ))
                .build();

        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(query, ProductDocument.class);
        List<ProductDocument> items = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toCollection(ArrayList::new));

        enrichCatalogPricing(items);

        return WeatherCategoryRecommendationPojo.builder()
                .sectionTitle("Goi y theo thoi tiet cho danh muc")
                .category(categoryCode)
                .weatherContext(weatherContext)
                .items(items)
                .build();
    }

    public WeatherCategoryRecommendationPojo recommendByWeather(
            Double latitude,
            Double longitude,
            int limit
    ) {
        WeatherContextPojo weatherContext = weatherContextService.resolve(latitude, longitude);
        int targetTemp = weatherContext.getTemperature() != null
                ? (int) Math.round(weatherContext.getTemperature())
                : 25;

        Pageable pageable = PageRequest.of(0, Math.max(1, limit));
        Query query = NativeQuery.builder()
                .withPageable(pageable)
                .withQuery(q -> q.bool(b -> b
                        .filter(f -> f.term(t -> t.field("status").value("PUBLISHED")))
                        .should(s -> s.term(t -> t
                                .field("weatherTags")
                                .value(weatherContext.getWeatherTag())
                                .boost(2.0f)))
                        .should(s -> s.bool(tempBoost -> tempBoost
                                .must(m1 -> m1.range(r -> r
                                        .field("tempMin")
                                        .lte(JsonData.of(targetTemp))))
                                .must(m2 -> m2.range(r -> r
                                        .field("tempMax")
                                        .gte(JsonData.of(targetTemp))))
                                .boost(1.5f)))
                        .minimumShouldMatch("0")
                ))
                .build();

        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(query, ProductDocument.class);
        List<ProductDocument> items = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toCollection(ArrayList::new));

        enrichCatalogPricing(items);

        return WeatherCategoryRecommendationPojo.builder()
                .sectionTitle("Gợi ý theo thời tiết hôm nay")
                .weatherContext(weatherContext)
                .items(items)
                .build();
    }

    private void enrichCatalogPricing(List<ProductDocument> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (ProductDocument doc : items) {
            if (doc == null) {
                continue;
            }
            if (doc.getCurrentPrice() != null && doc.getHasDiscount() != null) {
                continue;
            }
            String barcode = doc.getBarcode();
            if (barcode == null || barcode.isBlank()) {
                continue;
            }
            productsRepository.findByBarcode(barcode.trim()).ifPresent(product -> {
                var pricing = productPricingSnapshotService.calculate(product);
                doc.setPrice(pricing.originalPrice());
                doc.setOriginalPrice(pricing.originalPrice());
                doc.setCurrentPrice(pricing.currentPrice());
                doc.setDiscountPercent(pricing.discountPercent());
                doc.setHasDiscount(pricing.hasDiscount());
            });
        }
    }
}
