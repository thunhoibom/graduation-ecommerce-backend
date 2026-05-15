package org.monostudio.search.repositories;

import org.monostudio.search.models.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, String> {
    List<ProductDocument> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String name, String description);
}
