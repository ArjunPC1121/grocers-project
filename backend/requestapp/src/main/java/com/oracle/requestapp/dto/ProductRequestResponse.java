/**
 * Component role: Defines a transport contract used at an API or service boundary. Keep it free of persistence and business side effects.
 *
 * Maintainer note: this file belongs to requestapp. See backend/requestapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.requestapp.dto;

import com.oracle.requestapp.entities.ProductRequest;
import com.oracle.requestapp.entities.RequestAction;
import com.oracle.requestapp.entities.RequestStatus;

import java.math.BigDecimal;

public record ProductRequestResponse(
        Integer requestId, Integer employeeId, Integer productId, RequestAction action,
        RequestStatus status, String description, String name, String brand, String category,
        String subCategory, BigDecimal price, Integer quantity, Integer discount, String tags,
        String searchAliases, Double unitValue, String unitType, Boolean active, String imageUrl,
        String imageFileName, String reason, String previousValues, String rejectionReason, Integer reviewedByAdminId) {

    public static ProductRequestResponse from(ProductRequest request) {
        return new ProductRequestResponse(request.getRequestId(), request.getEmployeeId(),
                request.getProductId(), request.getAction(), request.getStatus(), request.getDescription(),
                request.getName(), request.getBrand(), request.getCategory(), request.getSubCategory(),
                request.getPrice(), request.getQuantity(), request.getDiscount(), request.getTags(),
                request.getSearchAliases(), request.getUnitValue(), request.getUnitType(), request.getActive(),
                request.getImageUrl(), request.getImageFileName(), request.getReason(), request.getPreviousValues(), request.getRejectionReason(),
                request.getReviewedByAdminId());
    }
}
