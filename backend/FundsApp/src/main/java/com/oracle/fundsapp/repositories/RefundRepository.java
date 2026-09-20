package com.oracle.fundsapp.repositories;

import com.oracle.fundsapp.entities.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Integer> {
    Optional<Refund> findByOrderId(Integer orderId);
}
