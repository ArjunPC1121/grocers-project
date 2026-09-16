package com.oracle.orderapp.dtos.clients;
import java.util.List;
public record InventoryDecrementRequest(String operationKey, String orderNumber, List<InventoryItemRequest> items) {}
