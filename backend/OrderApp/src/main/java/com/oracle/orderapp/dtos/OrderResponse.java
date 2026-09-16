package com.oracle.orderapp.dtos;
import com.oracle.orderapp.entities.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
public record OrderResponse(String orderNumber, Integer customerId, Integer cartId, OrderStatus status,
        Double totalAmount, String deliveryAddress, Integer updatedByEmployeeId, String cancellationReason,
        LocalDateTime orderedAt, LocalDateTime updatedAt, List<OrderItemResponse> items) {}
