package com.oracle.fundsapp.repositories;

import com.oracle.fundsapp.entities.Funds;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FundsRepository extends JpaRepository<Funds, Integer> {
    Optional<Funds> findByUserId(int userId);
}
