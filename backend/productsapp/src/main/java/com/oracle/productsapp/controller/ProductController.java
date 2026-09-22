package com.oracle.productsapp.controller;

import java.util.List;

import com.oracle.productsapp.dtos.QuantityRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.oracle.productsapp.dtos.ProductRequest;
import com.oracle.productsapp.entities.Product;
import com.oracle.productsapp.services.abstractions.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("grocers/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @GetMapping
    public List<Product> getAll() {
        return productService.getAll();
    }

    @GetMapping("/{id}")
    public Product getById(@PathVariable Integer id) {
        return productService.getById(id);
    }
    @PutMapping("/{id}")
    public Product update(@PathVariable Integer id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/{productId}/reduce-quantity")
    public ResponseEntity<Product> reduceQuantity(
            @PathVariable Integer productId,
            @Valid @RequestBody QuantityRequest request) {

        return ResponseEntity.ok(
                productService.reduceQuantity(productId, request.quantity())
        );
    }

    @PostMapping("/{productId}/increase-quantity")
    public ResponseEntity<Product> increaseQuantity(
            @PathVariable Integer productId,
            @Valid @RequestBody QuantityRequest request) {

        return ResponseEntity.ok(
                productService.increaseQuantity(productId, request.quantity())
        );
    }

    



}
