package com.oracle.orderapp.repositories;

import com.oracle.orderapp.entities.CancellationAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CancellationAttemptRepository extends JpaRepository<CancellationAttempt, Integer> {
    Optional<CancellationAttempt> findByOperationKey(String operationKey);
}
