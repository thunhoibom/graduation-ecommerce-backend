package org.monostudio.shipping.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaShippingProducer {
    private final KafkaTemplate<String, ShipmentTrackingEvent> kafkaTemplate;

    public void publish(ShipmentTrackingEvent event) {
        kafkaTemplate.send(KafkaShippingConfig.SHIPMENT_TRACKING_TOPIC, String.valueOf(event.getOrderId()), event);
        log.info("Published shipment tracking event for orderId: {}, status: {}", event.getOrderId(), event.getStatus());
    }
}
