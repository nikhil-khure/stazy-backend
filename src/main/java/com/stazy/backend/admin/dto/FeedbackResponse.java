package com.stazy.backend.admin.dto;

import com.stazy.backend.common.enums.FeedbackScope;
import com.stazy.backend.common.enums.FeedbackVisibilityStatus;
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
        boolean authenticated,
        boolean published,
        FeedbackVisibilityStatus visibilityStatus,
        OffsetDateTime createdAt
) {
}
