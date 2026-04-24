package com.stazy.backend.auth.dto;

import com.stazy.backend.common.enums.AuthProvider;
import com.stazy.backend.common.enums.RoleName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OAuthLoginRequest(
        @NotNull(message = "Provider is required.") AuthProvider provider,
        @NotNull(message = "Role is required.") RoleName role,
        @NotBlank(message = "Credential is required.") String credential,
        @NotBlank(message = "Mode is required.") String mode
) {
}
