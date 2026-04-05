package com.stazy.backend.booking.dto;

import com.stazy.backend.common.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RentPaymentResponse(
        UUID id,
        UUID activeStayId,
        String listingTitle,
        String roomCode,
        String ownerUserCode,
        String ownerName,
        String studentUserCode,
        String studentName,
        BigDecimal amount,
        LocalDate periodStart,
        LocalDate periodEnd,
        LocalDate dueDate,
        OffsetDateTime paidAt,
        PaymentStatus status,
        String reminderMessage,
        String notes
) {
}
