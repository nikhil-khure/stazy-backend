package com.stazy.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record OwnerSignupRequest(
        @NotBlank(message = "Name is required.") String name,
        String mobile,
        @NotBlank(message = "Email is required.") @Email(message = "Enter a valid email address.") String email,
        @NotBlank(message = "Password is required.") String password,
        @NotBlank(message = "Confirm password is required.") String confirmPassword
) {
}
