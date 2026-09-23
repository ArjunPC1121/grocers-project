package com.oracle.orderapp.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Complete, employee-facing order information for fulfilment operations. */
public record EmployeeOrderDetails(
        Integer id,
        String orderNumber,
        Integer customerId,
        OrderCustomerDetails customer,
        String status,
        BigDecimal totalAmount,
        String deliveryAddress,
        List<OrderItemDetails> items,
        String cancellationReason,
        LocalDateTime orderedAt,
        LocalDateTime updatedAt
) { }
