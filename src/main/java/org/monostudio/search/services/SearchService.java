package org.monostudio.search.services;

import co.elastic.clients.elasticsearch._types.FieldValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.monostudio.api.models.DataPagePojo;
import org.monostudio.api.models.WeatherCategoryRecommendationPojo;
import org.monostudio.api.models.WeatherContextPojo;
import org.monostudio.search.models.ProductDocument;
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
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SearchService {

    private static final int MAX_EXCLUDE_IDS = 100;

    private final ProductSearchRepository productSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final WeatherContextService weatherContextService;

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
        return searchProducts(keyword, minPrice, maxPrice, category, status, pageIndex, pageSize, sort, null);
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
        log.info("[ES DEBUG] Searching with keyword: '{}', category: '{}', price: {}-{}", 
                 keyword, category, minPrice, maxPrice);

        // Map JPA-style sort properties to Elasticsearch fields (name is analyzed text → sort on name.keyword)
        Sort esSort = toElasticsearchSort(sort);

        Pageable pageable = PageRequest.of(pageIndex, pageSize, esSort);

        var queryBuilder = NativeQuery.builder()
                .withPageable(pageable)
                .withQuery(q -> q
                        .bool(b -> {
                            // Full-text search on 'all' field
                            if (keyword != null && !keyword.isBlank()) {
                                b.must(m -> m.match(mt -> mt.field("all").query(keyword)));
                            }
                            
                            // Filter by status
                            if (status != null) {
                                b.filter(f -> f.term(t -> t.field("status").value(status)));
                            }

                            // Filter by category
                            if (category != null && !category.isBlank()) {
                                b.filter(f -> f.term(t -> t.field("categoryCodes").value(category)));
                            }

                            // Range filter for price
                            if (minPrice != null || maxPrice != null) {
                                b.filter(f -> f.range(r -> {
                                    if (minPrice != null) r.gte(co.elastic.clients.json.JsonData.of(minPrice));
                                    if (maxPrice != null) r.lte(co.elastic.clients.json.JsonData.of(maxPrice));
                                    return r.field("price");
                                }));
                            }

                            applyExcludeProductIds(b, excludeIds);
                            
                            return b;
                        })
                );

        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(queryBuilder.build(), ProductDocument.class);
        
        List<ProductDocument> items = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
        
        log.info("[ES DEBUG] Search finished. Hits: {}", searchHits.getTotalHits());

        return new DataPagePojo<>(items, pageIndex, searchHits.getTotalHits(), pageSize);
    }

    /**
     * Personalized "for you" rail: same keyword match as search, optional boost from category codes
     * derived from recent PRODUCT_VIEW events, and excludes current result page product ids.
     */
    public DataPagePojo<ProductDocument> searchForYou(
            String keyword,
            List<String> excludeIds,
            List<String> categoryBoostCodes,
            String status,
            int pageIndex,
            int pageSize,
            Sort sort
    ) {
        if (keyword == null || keyword.isBlank()) {
            return new DataPagePojo<>(List.of(), pageIndex, 0L, pageSize);
        }

        Sort esSort = toElasticsearchSort(sort);

        Pageable pageable = PageRequest.of(pageIndex, pageSize, esSort);

        List<String> boosts = categoryBoostCodes == null ? List.of() : categoryBoostCodes;

        var queryBuilder = NativeQuery.builder()
                .withPageable(pageable)
                .withQuery(q -> q
                        .bool(b -> {
                            b.must(m -> m.match(mt -> mt.field("all").query(keyword)));
                            if (status != null) {
                                b.filter(f -> f.term(t -> t.field("status").value(status)));
                            }
                            for (String cat : boosts) {
                                if (cat != null && !cat.isBlank()) {
                                    b.should(s -> s.term(t -> t.field("categoryCodes").value(cat).boost(2.0f)));
                                }
                            }
                            b.minimumShouldMatch("0");
                            applyExcludeProductIds(b, excludeIds);
                            return b;
                        })
                );

        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(queryBuilder.build(), ProductDocument.class);
        List<ProductDocument> items = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());

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

        return WeatherCategoryRecommendationPojo.builder()
                .sectionTitle("Gợi ý theo thời tiết hôm nay")
                .weatherContext(weatherContext)
                .items(items)
                .build();
    }
}
