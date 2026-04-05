package com.stazy.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminQueryCreateRequest(
        String subject,
        @NotBlank(message = "Message is required.") String message
) {
}
