package com.stazy.backend.admin.dto;

import com.stazy.backend.common.enums.HiringRequestStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminHiringRequestResponse(
        UUID id,
        String fullName,
        String mobileNumber,
        String email,
        String resumeUrl,
        HiringRequestStatus status,
        String reviewNotes,
        OffsetDateTime createdAt
) {
}
