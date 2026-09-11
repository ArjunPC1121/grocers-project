package com.oracle.orderapp.entities;

public enum OrderStatus {
    CREATED,
    PENDING_STOCK,
    PAYMENT_PENDING,
    PAYMENT_FAILED,
    STOCK_REJECTED,
    PLACED,
    SHIPPED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED
}
