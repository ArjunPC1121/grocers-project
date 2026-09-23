package com.oracle.userapp.repositories;

import com.oracle.userapp.entities.CustomerOrderSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerOrderSummaryRepository
        extends JpaRepository<CustomerOrderSummary, Integer> {

    List<CustomerOrderSummary> findByCustomerIdOrderByCheckedOutAtDesc(
            Integer customerId
    );
}