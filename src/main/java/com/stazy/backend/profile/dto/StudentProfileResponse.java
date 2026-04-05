package com.stazy.backend.profile.dto;

public record StudentProfileResponse(
        String userCode,
        String displayName,
        String email,
        String mobileNumber,
        String collegeName,
        String prn,
        String enrollmentNumber,
        String currentLocation,
        String profilePhotoUrl,
        int completionPercentage,
        boolean profileComplete,
        boolean identityVerified
) {
}
