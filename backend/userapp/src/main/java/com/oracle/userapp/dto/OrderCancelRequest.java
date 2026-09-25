package com.oracle.userapp.dto;

// Carries the refund amount and order reference when an order is cancelled.
public record OrderCancelRequest(Double amount, String reference) {
}
