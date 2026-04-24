package com.stazy.backend.admin.dto;

import com.stazy.backend.common.enums.FeedbackScope;
import com.stazy.backend.common.enums.FeedbackVisibilityStatus;
import com.stazy.backend.common.enums.RoleName;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FeedbackResponse(
        UUID id,
        FeedbackScope feedbackScope,
        Integer rating,
        String message,
        String displayName,
        String email,
        String location,
        String targetUserCode,
        String profilePhotoUrl,
        boolean authenticated,
        boolean published,
        FeedbackVisibilityStatus visibilityStatus,
        OffsetDateTime createdAt,
        RoleName userRole
) {
}
