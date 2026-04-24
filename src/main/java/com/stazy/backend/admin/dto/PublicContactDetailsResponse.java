package com.stazy.backend.admin.dto;

public record PublicContactDetailsResponse(
        String displayName,
        String email,
        String mobileNumber,
        String city
) {
}
