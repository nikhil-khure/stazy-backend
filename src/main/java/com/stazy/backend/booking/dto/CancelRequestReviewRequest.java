package com.stazy.backend.booking.dto;

public record CancelRequestReviewRequest(
        boolean accept,
        String ownerReason
) {
}
