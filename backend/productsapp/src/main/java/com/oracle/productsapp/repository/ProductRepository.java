package com.oracle.productsapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.oracle.productsapp.entities.Product;

public interface ProductRepository extends JpaRepository<Product, Integer> {
}
