package com.oracle.userapp.events;

import com.oracle.userapp.dto.OrderCheckedOutEvent;
import com.oracle.userapp.entities.CustomerOrderSummary;
import com.oracle.userapp.repositories.CustomerOrderSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
// Listens for completed orders and saves them for the customer's order-history page.
public class OrderCheckoutConsumer {

    private final CustomerOrderSummaryRepository customerOrderSummaryRepository;

    @Transactional
    @KafkaListener(
            topics = "order-checked-out",
            groupId = "${app.kafka.order-summary-group}"
    )
    // Converts one checkout event from Kafka into a local order summary.
    public void consume(OrderCheckedOutEvent event) {
        CustomerOrderSummary summary = new CustomerOrderSummary(
                event.orderId(),
                event.customerId(),
                event.orderNumber(),
                event.totalAmount(),
                event.status(),
                event.checkedOutAt()
        );

        customerOrderSummaryRepository.save(summary);
    }
}
