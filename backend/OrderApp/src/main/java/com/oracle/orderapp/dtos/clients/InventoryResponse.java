package com.oracle.orderapp.dtos.clients;
import java.util.List;
public record InventoryResponse(String orderNumber, String status, List<InventoryItemResponse> items) {}
