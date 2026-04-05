package com.stazy.backend.booking.dto;

import com.stazy.backend.common.enums.ActiveStayStatus;
import com.stazy.backend.common.enums.PaymentStatus;
import com.stazy.backend.common.enums.RoomKind;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ActiveStayResponse(
        UUID id,
        UUID listingId,
        String listingTitle,
        String listingLocation,
        RoomKind roomKind,
        String roomCode,
        String ownerUserCode,
        String ownerName,
        String ownerEmail,
        String ownerPhone,
        String studentUserCode,
        String studentName,
        String studentCollegeName,
        String studentEnrollmentNumber,
        String studentCurrentLocation,
        String studentEmail,
        String studentPhone,
        LocalDate joinDate,
        BigDecimal monthlyRent,
        ActiveStayStatus status,
        PaymentStatus currentPaymentStatus,
        LocalDate nextDueDate,
        String reminderMessage
) {
}
