package com.stazy.backend.profile.dto;

public record OwnerProfileResponse(
        String userCode,
        String displayName,
        String email,
        String mobileNumber,
        String panNumber,
        String pgName,
        Long cityId,
        String cityName,
        String addressLineOne,
        String locality,
        String pincode,
        String profilePhotoUrl,
        int completionPercentage,
        boolean profileComplete,
        boolean identityVerified
) {
}
