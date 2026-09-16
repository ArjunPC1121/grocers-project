package com.oracle.orderapp.entities;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class OrderLifecycleTest {
    @Test
    void newOrderDefaultsToPlaced() {
        assertEquals(OrderStatus.PLACED, new Order().getStatus());
    }
}
