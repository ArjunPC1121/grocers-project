package com.oracle.adminapp.controllers;

import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.oracle.adminapp.clients.ProductAdminClient;
import com.oracle.adminapp.dto.ProductImageResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("grocers/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductAdminClient productAdminClient;

    @PostMapping(
            value = "/{productId}/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ProductImageResponse> uploadProductImage(
            @PathVariable Integer productId,
            @RequestParam("image") MultipartFile image) throws IOException {

        return ResponseEntity.ok(
                productAdminClient.uploadImage(productId, image)
        );
    }
}
