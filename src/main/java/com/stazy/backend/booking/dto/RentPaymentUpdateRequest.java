package com.stazy.backend.booking.dto;

import com.stazy.backend.common.enums.PaymentStatus;
import java.time.LocalDate;

public record RentPaymentUpdateRequest(
        PaymentStatus status,
        LocalDate dueDate,
        String reminderMessage,
        String notes
) {
}
