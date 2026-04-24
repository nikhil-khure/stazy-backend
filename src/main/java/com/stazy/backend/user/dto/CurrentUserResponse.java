package com.stazy.backend.user.dto;

import com.stazy.backend.common.enums.AccountStatus;
import com.stazy.backend.common.enums.RoleName;

public record CurrentUserResponse(
        String userCode,
        String displayName,
        String email,
        String mobileNumber,
        RoleName role,
        AccountStatus accountStatus,
        boolean profileComplete,
        int completionPercentage,
        boolean identityVerified,
        String profilePhotoUrl
) {
}
