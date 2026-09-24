package com.oracle.userapp.repositories;

import com.oracle.userapp.entities.CustomerOrderSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Provides database access to the order summaries shown to customers.
public interface CustomerOrderSummaryRepository
        extends JpaRepository<CustomerOrderSummary, Integer> {

    // Finds a customer's order history with the latest checkout first.
    List<CustomerOrderSummary> findByCustomerIdOrderByCheckedOutAtDesc(
            Integer customerId
    );
}
