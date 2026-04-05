package com.stazy.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record ReviewNoteRequest(
        @NotBlank(message = "Review note is required.") String reviewNotes
) {
}
