package com.stazy.backend.complaint.dto;

import com.stazy.backend.common.enums.ComplaintStatus;
import com.stazy.backend.common.enums.RoleName;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ComplaintResponse(
        UUID id,
        String title,
        String description,
        ComplaintStatus status,
        String currentResolutionSummary,
        RoleName createdByRoleCode,
        RoleName againstRoleCode,
        String complainantUserCode,
        String complainantName,
        String againstUserCode,
        String againstName,
        UUID relatedListingId,
        UUID relatedStayId,
        OffsetDateTime closedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<ComplaintMessageResponse> messages
) {
}
