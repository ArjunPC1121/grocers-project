package com.oracle.orderapp.events;

import com.oracle.orderapp.dtos.OrderCheckedOutEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderCheckoutEventPublisher {

    private static final String TOPIC = "order-checked-out";

    private final KafkaTemplate<String, OrderCheckedOutEvent> kafkaTemplate;

    public void publish(OrderCheckedOutEvent event) {
        kafkaTemplate.send(TOPIC, event.orderId().toString(), event);
    }
}