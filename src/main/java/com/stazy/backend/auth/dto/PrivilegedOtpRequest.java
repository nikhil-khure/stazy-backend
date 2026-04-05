package com.stazy.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record PrivilegedOtpRequest(
        @NotBlank(message = "Admin ID is required.") String loginId,
        @NotBlank(message = "Password is required.") String password,
        @NotBlank(message = "Secret code is required.") String secretCode
) {
}
