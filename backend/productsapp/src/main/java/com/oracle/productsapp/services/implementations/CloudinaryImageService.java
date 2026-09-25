
package com.oracle.productsapp.services.implementations;

import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CloudinaryImageService {

    private final Cloudinary cloudinary;

    public Map upload(MultipartFile image) {
        // Validate the upload before sending bytes to the external image service.
        if (image.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }

        if (image.getContentType() == null
                || !image.getContentType().startsWith("image/")) {

            throw new IllegalArgumentException("Only image files are allowed");
        }

        try {
            // Store product images under one Cloudinary folder; the response includes
            // a public URL (secure_url) and a management identifier (public_id).
            return cloudinary.uploader().upload(
                    image.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "grocers/products",
                            "resource_type", "image"
                    )
            );
        } catch (IOException exception) {
            throw new RuntimeException("Image upload failed", exception);
        }
    }
}
