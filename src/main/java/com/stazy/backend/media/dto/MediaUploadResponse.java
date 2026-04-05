package com.stazy.backend.media.dto;

public record MediaUploadResponse(
        String url,
        String publicId,
        String resourceType,
        String format,
        long bytes,
        String originalFilename
) {
}
