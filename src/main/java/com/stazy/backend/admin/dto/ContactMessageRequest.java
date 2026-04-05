package com.stazy.backend.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ContactMessageRequest(
        @NotBlank(message = "Name is required.") String fullName,
        @NotBlank(message = "Email is required.") @Email(message = "Enter a valid email address.") String email,
        @NotBlank(message = "Message is required.") String message
) {
}
