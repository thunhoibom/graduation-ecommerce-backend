package org.monostudio.ordering.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the Kafka topic used for order lifecycle events.
 */
@Configuration
public class KafkaOrderConfig {

    /** Name of the Kafka topic for order events. */
    public static final String ORDER_TOPIC = "order-events";

    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name(ORDER_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
