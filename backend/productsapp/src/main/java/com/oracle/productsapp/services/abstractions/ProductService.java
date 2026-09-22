package com.oracle.productsapp.services.abstractions;

import java.util.List;

import com.oracle.productsapp.dtos.ProductRequest;
import com.oracle.productsapp.dtos.ProductSearchResult;
import com.oracle.productsapp.entities.Product;

public interface ProductService {

    Product create(ProductRequest request);

    List<Product> getAll();

    Product getById(Integer id);

    Product update(Integer id, ProductRequest request);

    void delete(Integer id);

    Product reduceQuantity(
            Integer productId,
            Integer quantity
    );

    Product increaseQuantity(
            Integer productId,
            Integer quantity
    );

    List<ProductSearchResult> search(
            String query,
            int limit
    );
}