package org.monostudio.search.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.monostudio.search.models.ShipmentTrackingDocument;
import org.monostudio.search.repositories.ShipmentTrackingSearchRepository;
import org.monostudio.shipping.kafka.KafkaShippingConfig;
import org.monostudio.shipping.kafka.ShipmentTrackingEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaShippingConsumer {

    private final ShipmentTrackingSearchRepository searchRepository;

    @KafkaListener(topics = KafkaShippingConfig.SHIPMENT_TRACKING_TOPIC, groupId = "shipping-tracking-group")
    public void consumeTrackingEvent(ShipmentTrackingEvent event) {
        log.info("Received shipment tracking event for orderId: {}", event.getOrderId());

        try {
            ShipmentTrackingDocument doc = ShipmentTrackingDocument.builder()
                    .id(String.valueOf(event.getTrackingId()))
                    .orderId(event.getOrderId())
                    .trackingNumber(event.getTrackingNumber())
                    .shipperCode(event.getShipperCode())
                    .status(event.getStatus())
                    .location(event.getLocation())
                    .description(event.getDescription())
                    .eventTime(event.getEventTime())
                    .receivedAt(Instant.now())
                    .build();
            
            searchRepository.save(doc);
            log.info("Indexed shipment tracking event: {}", doc.getId());
        } catch (Exception e) {
            log.error("Error indexing shipment tracking event: {}", event, e);
        }
    }
}
