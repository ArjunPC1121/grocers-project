package com.oracle.orderapp.dtos;
import java.util.List;
public record OrderReportSummary(long orderCount, Double totalRevenue, List<OrderReportRow> orders) {}
