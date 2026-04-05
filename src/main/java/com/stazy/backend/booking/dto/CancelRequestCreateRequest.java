package com.stazy.backend.booking.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelRequestCreateRequest(
        @NotBlank(message = "Reason is required.") String reason
) {
}
