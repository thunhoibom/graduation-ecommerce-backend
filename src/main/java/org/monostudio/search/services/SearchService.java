package org.monostudio.search.services;

import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
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
        log.info("[ES DEBUG] Searching with keyword: '{}', category: '{}', price: {}-{}", 
                 keyword, category, minPrice, maxPrice);

        // Map JPA-style sort properties to Elasticsearch fields
        Sort esSort = Sort.by(sort.stream().map(order -> {
            String property = order.getProperty();
            if (property.equals("productCategory.name")) property = "categoryName";
            return new Sort.Order(order.getDirection(), property);
        }).collect(Collectors.toList()));

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
                .sectionTitle("Goi y theo thoi tiet hom nay")
                .weatherContext(weatherContext)
                .items(items)
                .build();
    }
}
