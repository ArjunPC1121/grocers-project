package com.oracle.userapp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderCheckedOutEvent(
        Integer orderId,
        Integer customerId,
        String orderNumber,
        BigDecimal totalAmount,
        String status,
        LocalDateTime checkedOutAt
) {
}