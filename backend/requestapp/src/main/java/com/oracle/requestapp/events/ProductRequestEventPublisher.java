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