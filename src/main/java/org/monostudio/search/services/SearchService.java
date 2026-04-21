package org.monostudio.search.services;

import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.monostudio.api.models.DataPagePojo;
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

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SearchService {

    private final ProductSearchRepository productSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

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
}
