package com.stazy.backend.integration.cloudinary;

public record UploadedAsset(
        String url,
        String publicId,
        String resourceType,
        String format,
        long bytes,
        String originalFilename
) {
}
