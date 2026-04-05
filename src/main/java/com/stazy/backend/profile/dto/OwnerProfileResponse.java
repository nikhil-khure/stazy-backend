package com.stazy.backend.profile.dto;

public record OwnerProfileResponse(
        String userCode,
        String displayName,
        String email,
        String mobileNumber,
        String panNumber,
        String pgName,
        String businessName,
        String addressLineOne,
        String addressLineTwo,
        String locality,
        String pincode,
        String profilePhotoUrl,
        int completionPercentage,
        boolean profileComplete,
        boolean identityVerified
) {
}
