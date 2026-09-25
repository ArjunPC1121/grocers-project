package com.oracle.productsapp.services.abstractions;

import java.util.List;

import com.oracle.productsapp.dtos.ProductRequest;
import com.oracle.productsapp.dtos.ProductSearchResult;
import com.oracle.productsapp.entities.Product;
import com.oracle.productsapp.entities.ProductCategory;
import org.springframework.web.multipart.MultipartFile;

public interface ProductService {

    Product create(ProductRequest request);

    /**
     * Creates a fully searchable product and stores the supplied product image
     * in the same request. Intended for catalogue administration/imports.
     */
    Product createWithImage(ProductRequest request, MultipartFile image);

    List<Product> getAll();

    List<Product> getActiveByCategory(ProductCategory category);

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
    Product uploadImage(Integer productId, MultipartFile image);
}
