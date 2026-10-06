/**
 * Component role: Defines the HTTP boundary for this service. It accepts transport input, reads trusted gateway identity headers where required, and delegates business work to the service layer.
 *
 * Maintainer note: this file belongs to adminapp. See backend/adminapp/README.md for features, API contracts, configuration, and integration rules.
 */
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
