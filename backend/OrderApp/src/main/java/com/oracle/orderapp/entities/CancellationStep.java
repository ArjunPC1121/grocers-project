package com.oracle.orderapp.entities;

public enum CancellationStep {
    STARTED, INVENTORY_RESTORED, FUNDS_REFUNDED, ORDER_CANCELLED, COMPLETED, FAILED
}
