package com.oracle.employeeapp.dtos;

public record ProductRequestSummary(Integer requestId, Integer employeeId, Integer productId, String action,
                                    String status, String description, Integer quantity,
                                    String rejectionReason, Integer reviewedByAdminId) { }
