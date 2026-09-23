package com.oracle.adminapp.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record DashboardResponse(
        int totalProducts,
        int totalUsers,
        int inventoryUnits,
        BigDecimal inventoryValue,
        int lowStockProducts,
        int activeEmployees,
        int pendingRequests,
        int totalOrders,
        BigDecimal revenue,
        BigDecimal averageOrderValue,
        int fulfilledOrders,
        int fulfilmentRate,
        Map<String, Integer> orderStatuses,
        Map<String, Integer> requestStatuses,
        Map<String, Integer> categoryInventory,
        List<Map<String, Object>> recentOrders,
        List<Map<String, Object>> recentRequests,
        List<Map<String, Object>> lowStockItems) {
}
