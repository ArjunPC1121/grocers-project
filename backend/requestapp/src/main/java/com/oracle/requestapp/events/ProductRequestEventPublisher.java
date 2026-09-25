/**
 * Component role: Handles an asynchronous cross-service event. Event payloads should remain backward-compatible because producers and consumers deploy independently.
 *
 * Maintainer note: this file belongs to requestapp. See backend/requestapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.requestapp.events;

import com.oracle.requestapp.dto.ProductRequestCreatedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class ProductRequestEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;

    public ProductRequestEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${app.kafka.product-request-topic}") String topic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
    }

    public void publish(ProductRequestCreatedEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);

            // The request ID is the Kafka key. It keeps all notifications for a given
            // request ordered and gives consumers a stable idempotency identifier.
            kafkaTemplate.send(
                    topic,
                    event.requestId().toString(),
                    eventJson
            );
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Could not convert product-request event to JSON",
                    exception
            );
        }
    }
}
