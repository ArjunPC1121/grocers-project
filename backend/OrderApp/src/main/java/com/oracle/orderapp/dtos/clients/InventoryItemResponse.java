package com.oracle.orderapp.dtos.clients;
public record InventoryItemResponse(Integer productId, String productName, Integer quantity, Double unitPrice, Double subtotal) {}
