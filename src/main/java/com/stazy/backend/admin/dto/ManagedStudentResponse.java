package com.stazy.backend.admin.dto;

import com.stazy.backend.common.enums.AccountStatus;
import java.util.UUID;

public record ManagedStudentResponse(
        UUID userId,
        String userCode,
        String displayName,
        String email,
        String mobileNumber,
        boolean identityVerified,
        AccountStatus accountStatus,
        String currentCity,
        long activeComplaints,
        long resolvedComplaints,
        String collegeName,
        String prn,
        Integer completionPercentage
) {
}
