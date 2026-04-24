package com.stazy.backend.notification.dto;

import com.stazy.backend.common.enums.NotificationType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType notificationType,
        String title,
        String message,
        String actionPath,
        boolean read,
        OffsetDateTime readAt,
        OffsetDateTime createdAt
) {
}
