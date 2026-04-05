package com.stazy.backend.profile.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateOwnerProfileRequest(
        @NotBlank(message = "Full name is required.") String displayName,
        @NotBlank(message = "Email is required.") @Email(message = "Enter a valid email address.") String email,
        String mobileNumber,
        String panNumber,
        String pgName,
        String businessName,
        String addressLineOne,
        String addressLineTwo,
        String locality,
        String pincode,
        String profilePhotoUrl,
        String profilePhotoPublicId
) {
}
