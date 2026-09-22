package com.oracle.orderapp.repository;

import com.oracle.orderapp.entities.Order;
import com.oracle.orderapp.entities.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Integer> {

    List<Order> findByCustomerId(Integer customerId);
    List<Order> findByStatus(OrderStatus status);
}
