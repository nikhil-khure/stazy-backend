package com.stazy.backend.booking.dto;

import com.stazy.backend.common.enums.BookingRequestStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BookingRequestResponse(
        UUID id,
        UUID listingId,
        String listingTitle,
        String listingLocation,
        String ownerUserCode,
        String ownerName,
        String studentUserCode,
        String studentName,
        String studentCollegeName,
        BigDecimal requestedRent,
        int availableCapacity,
        int totalCapacity,
        String message,
        BookingRequestStatus status,
        String rejectionReason,
        OffsetDateTime requestedAt
) {
}
