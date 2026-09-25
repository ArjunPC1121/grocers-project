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
        if (image.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }

        try {
            byte[] imageBytes = image.getBytes();
            if (!isImage(image.getContentType(), imageBytes)) {
                throw new IllegalArgumentException("Only image files are allowed");
            }

            return cloudinary.uploader().upload(
                    imageBytes,
                    ObjectUtils.asMap(
                            "folder", "grocers/products",
                            "resource_type", "image"
                    )
            );
        } catch (IOException exception) {
            throw new RuntimeException("Image upload failed", exception);
        }
    }

    private boolean isImage(String contentType, byte[] bytes) {
        return contentType != null && contentType.startsWith("image/")
                || isJpeg(bytes)
                || isPng(bytes)
                || isGif(bytes)
                || isWebp(bytes);
    }

    private boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] bytes) {
        return bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A;
    }

    private boolean isGif(byte[] bytes) {
        return bytes.length >= 6
                && bytes[0] == 'G'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == '8'
                && (bytes[4] == '7' || bytes[4] == '9')
                && bytes[5] == 'a';
    }

    private boolean isWebp(byte[] bytes) {
        return bytes.length >= 12
                && bytes[0] == 'R'
                && bytes[1] == 'I'
                && bytes[2] == 'F'
                && bytes[3] == 'F'
                && bytes[8] == 'W'
                && bytes[9] == 'E'
                && bytes[10] == 'B'
                && bytes[11] == 'P';
    }
}
