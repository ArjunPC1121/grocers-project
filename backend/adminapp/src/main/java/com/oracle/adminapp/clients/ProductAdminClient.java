/**
 * Component role: Encapsulates a call to another service so that endpoint details do not leak into controllers or domain logic.
 *
 * Maintainer note: this file belongs to adminapp. See backend/adminapp/README.md for features, API contracts, configuration, and integration rules.
 */
package com.oracle.adminapp.clients;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.oracle.adminapp.dto.ProductImageResponse;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProductAdminClient {

    private final RestTemplate restTemplate;

    @Value("${services.products-url}")
    private String productServiceUrl;

    public ProductImageResponse uploadImage(
            Integer productId,
            MultipartFile image) throws IOException {

        ByteArrayResource imageResource =
                new ByteArrayResource(image.getBytes()) {
                    @Override
                    public String getFilename() {
                        return image.getOriginalFilename();
                    }
                };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", imageResource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        return restTemplate.postForObject(
                productServiceUrl + "/{productId}/image",
                new HttpEntity<>(body, headers),
                ProductImageResponse.class,
                productId
        );
    }
}
