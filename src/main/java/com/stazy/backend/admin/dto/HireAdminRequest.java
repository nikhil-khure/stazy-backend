package com.stazy.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record HireAdminRequest(
        @NotBlank(message = "Admin ID is required.") String adminId,
        @NotBlank(message = "Password is required.") String password,
        @NotBlank(message = "Secret code is required.") String secretCode,
        @NotNull(message = "Assigned city is required.") Long cityId,
        String reviewNotes
) {
}
