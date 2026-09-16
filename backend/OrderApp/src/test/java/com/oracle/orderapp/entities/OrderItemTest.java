package com.oracle.orderapp.entities;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class OrderItemTest {
    @Test
    void calculatesRoundedDoubleSubtotal() {
        OrderItem item = new OrderItem();
        item.setQuantity(3);
        item.setUnitPrice(10.125d);

        item.recalculateSubtotal();

        assertEquals(30.38d, item.getSubtotal(), 0.001d);
    }
}
