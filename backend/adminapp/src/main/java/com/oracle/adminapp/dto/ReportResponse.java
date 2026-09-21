package com.oracle.adminapp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record ReportResponse(
        ReportPeriod period,
        LocalDate fromDate,
        LocalDate toDate,
        Integer productId,
        Integer customerId,
        int orderCount,
        BigDecimal revenue,
        List<Map<String, Object>> orders) {
}
