package com.oracle.orderapp.repositories;

import com.oracle.orderapp.entities.CheckoutAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CheckoutAttemptRepository extends JpaRepository<CheckoutAttempt, Integer> {
    Optional<CheckoutAttempt> findByIdempotencyKey(String idempotencyKey);
    Optional<CheckoutAttempt> findByOrderNumber(String orderNumber);
}
