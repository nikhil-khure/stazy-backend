package com.stazy.backend.auth.dto;

import com.stazy.backend.common.enums.RoleName;
import java.util.UUID;

public record AuthUserResponse(
        UUID id,
        String userCode,
        RoleName role,
        String displayName,
        String email,
        boolean profileComplete,
        int completionPercentage,
        boolean identityVerified
) {
}
