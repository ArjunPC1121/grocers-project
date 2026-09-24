package com.oracle.userapp.repositories;

import com.oracle.userapp.entities.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction,Long> {

    List<WalletTransaction> findByUserIdOrderByCreatedAtDesc(Integer userId);
}
