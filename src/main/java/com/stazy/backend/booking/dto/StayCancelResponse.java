package com.stazy.backend.booking.dto;

import com.stazy.backend.common.enums.CancelRequestStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StayCancelResponse(
        UUID id,
        UUID activeStayId,
        String listingTitle,
        String roomCode,
        String ownerUserCode,
        String ownerName,
        String studentUserCode,
        String studentName,
        String reason,
        String accountStatusSnapshot,
        CancelRequestStatus status,
        String ownerReason,
        OffsetDateTime requestedAt
) {
}
