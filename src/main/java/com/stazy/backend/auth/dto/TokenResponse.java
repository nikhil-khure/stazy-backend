package com.stazy.backend.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        AuthUserResponse user
) {
}
