package com.oracle.productsapp.services.implementations;


import java.util.List;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import com.oracle.productsapp.dtos.ProductRequest;
import com.oracle.productsapp.entities.Product;
import com.oracle.productsapp.exceptions.ResourceNotFoundException;
import com.oracle.productsapp.repository.ProductRepository;
import com.oracle.productsapp.services.abstractions.ProductService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

   
@Override
public Product create(ProductRequest request) {
    Product product = new Product();

    product.setName(request.name());
    product.setPrice(request.price());
    product.setDiscount(request.discount());
    product.setQuantity(request.quantity());

    return productRepository.save(product);
}


    @Override
    public List<Product> getAll() {
        return productRepository.findAll();
    }

    @Override
    public Product getById(Integer id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

 
  @Override
public Product update(Integer id, ProductRequest request) {
    Product product = getById(id);

    product.setName(request.name());
    product.setPrice(request.price());
    product.setDiscount(request.discount());
    product.setQuantity(request.quantity());

    return productRepository.save(product);
}

    @Override
    public void delete(Integer id) {
        productRepository.delete(getById(id));
    }

    @Override
    @Transactional
    public Product reduceQuantity(Integer productId, Integer quantity) {
        Product product = getById(productId);

        if (product.getQuantity() < quantity) {
            throw new IllegalArgumentException(
                    "Insufficient quantity for product: " + product.getName()
            );
        }

        product.setQuantity(product.getQuantity() - quantity);
        return productRepository.save(product);
    }

    @Override
    @Transactional
    public Product increaseQuantity(Integer productId, Integer quantity) {
        Product product = getById(productId);

        product.setQuantity(product.getQuantity() + quantity);
        return productRepository.save(product);
    }
       


    }
