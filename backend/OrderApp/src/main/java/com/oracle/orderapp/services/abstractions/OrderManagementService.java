package com.oracle.orderapp.services.abstractions;
import com.oracle.orderapp.dtos.*;
public interface OrderManagementService {
    OrderResponse updateStatus(Integer employeeId, String orderNumber, OrderStatusUpdateRequest request);
    OrderResponse cancel(Integer employeeId, String operationKey, String orderNumber, OrderCancellationRequest request);
}
