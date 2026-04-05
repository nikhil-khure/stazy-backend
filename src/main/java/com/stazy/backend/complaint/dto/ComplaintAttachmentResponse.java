package com.stazy.backend.complaint.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ComplaintAttachmentResponse(
        UUID id,
        String url,
        String mimeType,
        Long fileSizeBytes,
        OffsetDateTime createdAt
) {
}
