package org.monostudio.search.repositories;

import org.monostudio.search.models.BlogPostDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlogPostSearchRepository extends ElasticsearchRepository<BlogPostDocument, String> {
}
