package com.stazy.backend.profile.dto;

import com.stazy.backend.common.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateStudentProfileRequest(
        @NotBlank(message = "Full name is required.") String displayName,
        @NotBlank(message = "Email is required.") @Email(message = "Enter a valid email address.") String email,
        String mobileNumber,
        String collegeName,
        String prn,
        String enrollmentNumber,
        String currentLocation,
        Long cityId,
        String profilePhotoUrl,
        String profilePhotoPublicId,
        Gender gender
) {
}
