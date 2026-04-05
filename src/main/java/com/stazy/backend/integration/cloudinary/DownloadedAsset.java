package com.stazy.backend.integration.cloudinary;

import org.springframework.http.MediaType;

public record DownloadedAsset(
        byte[] bytes,
        MediaType mediaType,
        String fileName
) {
}
