package com.stazy.backend.auth.dto;

import com.stazy.backend.common.enums.RoleName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @NotBlank(message = "Login ID is required.") String loginId,
        @NotBlank(message = "Password is required.") String password,
        @NotNull(message = "Role is required.") RoleName role
) {
}
