package com.oracle.requestapp.dto;

import com.oracle.requestapp.entities.ProductRequest;
import com.oracle.requestapp.entities.RequestAction;
import com.oracle.requestapp.entities.RequestStatus;

import java.math.BigDecimal;

public record ProductRequestResponse(
        Integer requestId, Integer employeeId, Integer productId, RequestAction action,
        RequestStatus status, String description, String name, BigDecimal price,
        Integer quantity, Integer discount, String rejectionReason, Integer reviewedByAdminId) {

    public static ProductRequestResponse from(ProductRequest request) {
        return new ProductRequestResponse(request.getRequestId(), request.getEmployeeId(),
                request.getProductId(), request.getAction(), request.getStatus(), request.getDescription(),
                request.getName(), request.getPrice(), request.getQuantity(), request.getDiscount(),
                request.getRejectionReason(), request.getReviewedByAdminId());
    }
}
