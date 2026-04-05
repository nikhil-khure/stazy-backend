package com.stazy.backend.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdatePasswordRequest(
        @NotBlank(message = "Current password is required.") String currentPassword,
        @NotBlank(message = "New password is required.") String newPassword,
        @NotBlank(message = "Confirm password is required.") String confirmPassword
) {
}
