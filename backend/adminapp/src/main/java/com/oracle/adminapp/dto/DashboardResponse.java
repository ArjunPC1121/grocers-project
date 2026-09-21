package com.oracle.adminapp.dto;

import java.math.BigDecimal;

public record DashboardResponse(
        int totalProducts,
        int lowStockProducts,
        int activeEmployees,
        int pendingRequests,
        int totalOrders,
        BigDecimal revenue) {
}
