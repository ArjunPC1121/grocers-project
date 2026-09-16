package com.oracle.orderapp.repositories;

import com.oracle.orderapp.entities.Order;
import com.oracle.orderapp.entities.OrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Integer>, JpaSpecificationExecutor<Order> {
    @EntityGraph(attributePaths = "items") Optional<Order> findByOrderNumber(String orderNumber);
    @EntityGraph(attributePaths = "items") List<Order> findByCustomerIdOrderByOrderedAtDesc(Integer customerId);
    @EntityGraph(attributePaths = "items") List<Order> findByStatusOrderByOrderedAtDesc(OrderStatus status);
    @EntityGraph(attributePaths = "items") List<Order> findAllByOrderByOrderedAtDesc();
}
