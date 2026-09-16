package com.oracle.orderapp.repositories;

import com.oracle.orderapp.entities.Order;
import com.oracle.orderapp.entities.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
class OrderRepositoryTest {
    @Autowired OrderRepository repository;

    @Test
    void findsUserHistoryNewestFirst() {
        repository.save(order("ORD-1", LocalDateTime.of(2026, 9, 15, 10, 0)));
        repository.save(order("ORD-2", LocalDateTime.of(2026, 9, 16, 10, 0)));

        List<String> numbers = repository.findByCustomerIdOrderByOrderedAtDesc(41)
                .stream().map(Order::getOrderNumber).toList();

        assertEquals(List.of("ORD-2", "ORD-1"), numbers);
    }

    private Order order(String number, LocalDateTime at) {
        Order order = new Order();
        order.setOrderNumber(number);
        order.setCustomerId(41);
        order.setCartId(25);
        order.setStatus(OrderStatus.PLACED);
        order.setTotalAmount(10.0d);
        order.setDeliveryAddress("12 Market Road");
        order.setOrderedAt(at);
        return order;
    }
}
