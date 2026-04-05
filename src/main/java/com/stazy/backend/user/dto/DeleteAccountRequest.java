package com.stazy.backend.user.dto;

import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequest(
        @NotBlank(message = "Current password is required.") String currentPassword
) {
}
