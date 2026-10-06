/**
 * Component role: Defines a transport contract used at an API or service boundary. Keep it free of persistence and business side effects.
 *
 * Maintainer note: this file belongs to adminapp. See backend/adminapp/README.md for features, API contracts, configuration, and integration rules.
 */
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
