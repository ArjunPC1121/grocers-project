package com.oracle.orderapp.entities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderLifecycleTest {
    @Test
    void newOrderDefaultsToPlaced() {
        assertEquals(OrderStatus.PLACED, new Order().getStatus());
    }
}
