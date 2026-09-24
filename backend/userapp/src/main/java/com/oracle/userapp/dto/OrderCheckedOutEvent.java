package com.oracle.userapp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/*
This is the DTO which order passes after an order is checked out.
Kafka communication happens through this DTO.
 */
public record OrderCheckedOutEvent(
        Integer orderId,
        Integer customerId,
        String orderNumber,
        BigDecimal totalAmount,
        String status,
        LocalDateTime checkedOutAt
) {
}