package com.stazy.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record OtpVerifyRequest(
        @NotBlank(message = "Admin ID is required.") String loginId,
        @NotBlank(message = "OTP is required.") String otp
) {
}
