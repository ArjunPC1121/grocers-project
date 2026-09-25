package com.oracle.userapp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.oracle.userapp.entities.CustomerOrderSummary;

//Used for tracking of orders - Kafka implementation
public record CustomerOrderSummaryResponse(
        Integer orderId,
        String orderNumber,
        BigDecimal totalAmount,
        String status,
        LocalDateTime checkedOutAt
) {
    public static CustomerOrderSummaryResponse from(
            CustomerOrderSummary summary
    ) {
        return new CustomerOrderSummaryResponse(
                summary.getOrderId(),
                summary.getOrderNumber(),
                summary.getTotalAmount(),
                summary.getStatus(),
                summary.getCheckedOutAt()
        );
    }
}