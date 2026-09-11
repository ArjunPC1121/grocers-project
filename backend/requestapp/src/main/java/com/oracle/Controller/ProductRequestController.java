package com.oracle.Controller;


import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.oracle.Entity.ProductRequest;
import com.oracle.Entity.RequestAction;
import com.oracle.Repositories.ProductRequestRepository;

@RestController
@RequestMapping("/requests")
public class ProductRequestController {

    private final ProductRequestRepository repository;

    public ProductRequestController(ProductRequestRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ProductRequest create(
            @RequestParam int employeeId,
            @RequestParam int productId,
            @RequestParam RequestAction action,
            @RequestParam String description) {

        ProductRequest request = new ProductRequest(
                employeeId, productId, action, description);

        return repository.save(request);
    }

    @GetMapping
    public List<ProductRequest> getAll() {
        return repository.findAll();
    }
}
