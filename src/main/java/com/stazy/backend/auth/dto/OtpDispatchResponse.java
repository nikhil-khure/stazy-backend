package com.stazy.backend.auth.dto;

public record OtpDispatchResponse(
        String deliveryTarget,
        String otpForLocalDevelopment
) {
}
