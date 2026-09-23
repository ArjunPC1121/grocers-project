package com.oracle.userapp.dto;

import com.oracle.userapp.entities.CustomerOrderSummary;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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