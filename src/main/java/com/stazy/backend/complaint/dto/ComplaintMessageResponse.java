package com.stazy.backend.complaint.dto;

import com.stazy.backend.common.enums.ComplaintMessageType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ComplaintMessageResponse(
        UUID id,
        String authorUserCode,
        String authorName,
        ComplaintMessageType messageType,
        String message,
        OffsetDateTime createdAt,
        List<ComplaintAttachmentResponse> attachments
) {
}
