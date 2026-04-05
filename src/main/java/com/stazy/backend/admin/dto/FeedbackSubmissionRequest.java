package com.stazy.backend.admin.dto;

import com.stazy.backend.common.enums.FeedbackScope;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FeedbackSubmissionRequest(
        @NotNull(message = "Feedback scope is required.") FeedbackScope feedbackScope,
        @Min(value = 1, message = "Rating must be at least 1.") @Max(value = 5, message = "Rating must be at most 5.") Integer rating,
        @NotBlank(message = "Message is required.") String message,
        String location,
        String targetUserCode
) {
}
