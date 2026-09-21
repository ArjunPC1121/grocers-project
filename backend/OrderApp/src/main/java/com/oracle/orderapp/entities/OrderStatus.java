package com.oracle.orderapp.entities;

public enum OrderStatus {
    CREATED,
    PLACED,
    STOCK_REJECTED,
    PAYMENT_FAILED,
    CANCELLED,
    SHIPPED,
    OUT_FOR_DELIVERY,
    DELIVERED
}
