package com.oracle.productsapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.oracle.productsapp.entities.Product;
import com.oracle.productsapp.entities.ProductCategory;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    List<Product> findByCategoryAndActiveTrueOrderByNameAsc(ProductCategory category);
}
