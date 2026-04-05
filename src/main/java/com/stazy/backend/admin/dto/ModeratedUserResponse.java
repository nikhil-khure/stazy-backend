package com.stazy.backend.admin.dto;

import com.stazy.backend.common.enums.AccountStatus;
import com.stazy.backend.common.enums.RoleName;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ModeratedUserResponse(
        UUID userId,
        String userCode,
        String displayName,
        RoleName role,
        AccountStatus accountStatus,
        OffsetDateTime deletedAt
) {
}
