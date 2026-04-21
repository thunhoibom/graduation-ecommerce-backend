package org.monostudio.search.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class IndexEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    public static final String TOPIC = "search-indexing-topic";

    public void sendIndexEvent(String entityType, Long entityId, String operation) {
        IndexEvent event = IndexEvent.builder()
                .entityType(entityType)
                .entityId(entityId)
                .operation(operation)
                .build();
        
        log.info("Sending index event to Kafka: {}", event);
        kafkaTemplate.send(TOPIC, entityId.toString(), event);
    }
}
