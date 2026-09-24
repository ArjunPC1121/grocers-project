package com.oracle.adminapp.services;


import com.oracle.adminapp.dto.ProductRequestCreatedEvent;
import com.oracle.adminapp.entities.AdminNotification;
import com.oracle.adminapp.repositories.AdminNotificationRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class ProductRequestNotificationConsumer {

    private final AdminNotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    public ProductRequestNotificationConsumer(
            AdminNotificationRepository notificationRepository,
            ObjectMapper objectMapper
    ) {
        this.notificationRepository = notificationRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.kafka.product-request-topic}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(String eventJson) {
        try {
            ProductRequestCreatedEvent event =
                    objectMapper.readValue(
                            eventJson,
                            ProductRequestCreatedEvent.class
                    );

            if (notificationRepository.existsByRequestId(event.requestId())) {
                return;
            }

            String productName = event.productName() == null
                    ? "product #" + event.requestId()
                    : event.productName();

            String message = "Employee #" + event.employeeId()
                    + " submitted a " + event.action()
                    + " request for " + productName + ".";

            notificationRepository.save(
                    new AdminNotification(
                            event.requestId(),
                            event.employeeId(),
                            "New employee request",
                            message,
                            event.createdAt()
                    )
            );
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Could not read product-request Kafka event",
                    exception
            );
        }
    }
}