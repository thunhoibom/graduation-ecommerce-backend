package org.monostudio.search.repositories;

import org.monostudio.search.models.ShipmentTrackingDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShipmentTrackingSearchRepository extends ElasticsearchRepository<ShipmentTrackingDocument, String> {
    List<ShipmentTrackingDocument> findByOrderIdOrderByEventTimeDesc(Long orderId);
    List<ShipmentTrackingDocument> findByTrackingNumberOrderByEventTimeDesc(String trackingNumber);
}
