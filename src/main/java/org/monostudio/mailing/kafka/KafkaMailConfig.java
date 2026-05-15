package org.monostudio.mailing.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the Kafka topic used for mail event messages.
 * Spring will auto-create the topic on startup if it doesn't already exist.
 */
@Configuration
public class KafkaMailConfig {

    /** Name of the Kafka topic for mail events. */
    public static final String MAIL_TOPIC = "mail-events";

    @Bean
    public NewTopic mailEventsTopic() {
        return TopicBuilder.name(MAIL_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
