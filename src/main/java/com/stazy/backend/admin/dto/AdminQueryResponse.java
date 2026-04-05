package com.stazy.backend.admin.dto;

import com.stazy.backend.common.enums.AdminQueryStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminQueryResponse(
        UUID id,
        String adminUserCode,
        String subject,
        String message,
        AdminQueryStatus status,
        String replyMessage,
        OffsetDateTime createdAt,
        OffsetDateTime repliedAt
) {
}
