package com.oracle.orderapp.dtos;
public record OrderItemResponse(Integer productId, String productName, Integer quantity, Double unitPrice, Double subtotal) {}
