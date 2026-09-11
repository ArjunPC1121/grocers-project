package com.oracle.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.oracle.Entity.ProductRequest;


public interface ProductRequestRepository
        extends JpaRepository<ProductRequest, Integer > {
}