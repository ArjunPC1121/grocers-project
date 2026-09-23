package com.oracle.adminapp.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record DashboardResponse(
        int totalProducts,
        int lowStockProducts,
        int activeEmployees,
        int pendingRequests,
        int totalOrders,
        BigDecimal revenue,
        Map<String, Integer> orderStatuses,
        List<Map<String, Object>> recentOrders,
        List<Map<String, Object>> lowStockItems) {
}
