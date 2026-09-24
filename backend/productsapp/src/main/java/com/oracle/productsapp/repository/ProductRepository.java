package com.oracle.productsapp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.oracle.productsapp.dtos.ProductResponse;
import com.oracle.productsapp.entities.Product;
import com.oracle.productsapp.entities.ProductCategory;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    @Query("""
            SELECT new com.oracle.productsapp.dtos.ProductResponse(
                p.id, p.name, p.brand, p.category, p.subCategory,
                p.description, p.tags, p.searchAliases, p.unitValue,
                p.unitType, p.imageUrl, p.price, p.discount, p.quantity, p.active
            )
            FROM Product p
            ORDER BY p.name ASC, p.id ASC
            """)
    List<ProductResponse> findCatalog(Pageable pageable);

    @Query("""
            SELECT new com.oracle.productsapp.dtos.ProductResponse(
                p.id, p.name, p.brand, p.category, p.subCategory,
                p.description, p.tags, p.searchAliases, p.unitValue,
                p.unitType, p.imageUrl, p.price, p.discount, p.quantity, p.active
            )
            FROM Product p
            WHERE p.category = :category AND p.active = true
            ORDER BY p.name ASC, p.id ASC
            """)
    List<ProductResponse> findActiveCatalogByCategory(
            ProductCategory category,
            Pageable pageable
    );

    @Query("""
            SELECT new com.oracle.productsapp.dtos.ProductResponse(
                p.id, p.name, p.brand, p.category, p.subCategory,
                p.description, p.tags, p.searchAliases, p.unitValue,
                p.unitType, p.imageUrl, p.price, p.discount, p.quantity, p.active
            )
            FROM Product p
            WHERE p.id = :id
            """)
    Optional<ProductResponse> findPublicById(Integer id);
}
