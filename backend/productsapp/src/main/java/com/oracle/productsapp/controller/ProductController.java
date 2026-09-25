package com.oracle.productsapp.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import com.oracle.productsapp.dtos.ImageUploadResponse;
import com.oracle.productsapp.dtos.UploadedImageResponse;
import com.oracle.productsapp.dtos.ProductSearchResult;
import com.oracle.productsapp.dtos.QuantityRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.oracle.productsapp.dtos.ProductRequest;
import com.oracle.productsapp.entities.Product;
import com.oracle.productsapp.entities.ProductCategory;
import com.oracle.productsapp.services.abstractions.ProductService;
import com.oracle.productsapp.services.implementations.CloudinaryImageService;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("grocers/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CloudinaryImageService cloudinaryImageService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    /**
     * Temporary catalogue-entry endpoint for Insomnia: submit the product JSON
     * and its local image together as multipart/form-data.
     */
    @PostMapping(value = "/with-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Product> createWithImage(
            @RequestParam("product") String productJson,
            @RequestParam("image") MultipartFile image) {
        ProductRequest request = parseAndValidateProduct(productJson);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.createWithImage(request, image));
    }

    private ProductRequest parseAndValidateProduct(String productJson) {
        try {
            ProductRequest request = objectMapper.readValue(productJson, ProductRequest.class);
            Set<ConstraintViolation<ProductRequest>> violations = validator.validate(request);
            if (!violations.isEmpty()) {
                ConstraintViolation<ProductRequest> violation = violations.iterator().next();
                throw new IllegalArgumentException(
                        violation.getPropertyPath() + ": " + violation.getMessage()
                );
            }
            return request;
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("product must contain valid JSON", exception);
        }
    }

    @GetMapping
    public List<Product> getAll(
            @RequestParam(required = false) String category
    ) {
        return category == null || category.isBlank()
                ? productService.getAll()
                : productService.getActiveByCategory(ProductCategory.from(category));
    }

    /*
     * Literal /search endpoint is declared separately from /{id}.
     */
    @GetMapping("/search")
    public List<ProductSearchResult> search(
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return productService.search(query, limit);
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
    @PostMapping(
            value = "/{productId}/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ImageUploadResponse> uploadImage(
            @PathVariable Integer productId,
            @RequestParam("image") MultipartFile image) {

        Product product = productService.uploadImage(productId, image);

        return ResponseEntity.ok(
                new ImageUploadResponse(
                        product.getId(),
                        product.getImageUrl()
                )
        );
    }

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadedImageResponse> uploadRequestImage(
            @RequestParam("image") MultipartFile image) {
        Map uploadResult = cloudinaryImageService.upload(image);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UploadedImageResponse((String) uploadResult.get("secure_url")));
    }

    



}
