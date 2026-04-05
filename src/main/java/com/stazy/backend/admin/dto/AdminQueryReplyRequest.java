package com.stazy.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminQueryReplyRequest(
        @NotBlank(message = "Reply message is required.") String replyMessage
) {
}
