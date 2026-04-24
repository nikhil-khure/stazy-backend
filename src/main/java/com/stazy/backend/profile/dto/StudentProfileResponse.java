package com.stazy.backend.profile.dto;

import com.stazy.backend.common.enums.Gender;

public record StudentProfileResponse(
        String userCode,
        String displayName,
        String email,
        String mobileNumber,
        String collegeName,
        String prn,
        String enrollmentNumber,
        String currentLocation,
        Long cityId,
        String cityName,
        String profilePhotoUrl,
        int completionPercentage,
        boolean profileComplete,
        boolean identityVerified,
        Gender gender
) {
}
