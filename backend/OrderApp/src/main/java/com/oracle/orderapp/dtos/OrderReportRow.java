package com.oracle.orderapp.dtos;
import com.oracle.orderapp.entities.OrderStatus;
import java.time.LocalDateTime;
public record OrderReportRow(String orderNumber, Integer customerId, OrderStatus status, Double totalAmount, LocalDateTime orderedAt) {}
