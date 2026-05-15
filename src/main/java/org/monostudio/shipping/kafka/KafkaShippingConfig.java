package org.monostudio.shipping.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaShippingConfig {
    public static final String SHIPMENT_TRACKING_TOPIC = "shipment-tracking-topic";

    @Bean
    public NewTopic shipmentTrackingTopic() {
        return TopicBuilder.name(SHIPMENT_TRACKING_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
