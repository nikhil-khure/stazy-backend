package com.stazy.backend.notification.controller;

import com.stazy.backend.common.api.ApiResponse;
import com.stazy.backend.notification.dto.NotificationResponse;
import com.stazy.backend.notification.service.NotificationService;
import com.stazy.backend.security.StazyPrincipal;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/me")
    public ApiResponse<List<NotificationResponse>> myNotifications(@AuthenticationPrincipal StazyPrincipal principal) {
        return ApiResponse.ok("Notifications loaded successfully.", notificationService.myNotifications(principal.getUserId()));
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponse> markRead(
            @AuthenticationPrincipal StazyPrincipal principal,
            @PathVariable UUID notificationId
    ) {
        return ApiResponse.ok("Notification marked as read.", notificationService.markRead(principal.getUserId(), notificationId));
    }

    @DeleteMapping("/me/all")
    public ApiResponse<Void> clearAllNotifications(@AuthenticationPrincipal StazyPrincipal principal) {
        notificationService.clearAllNotifications(principal.getUserId());
        return ApiResponse.ok("All notifications cleared successfully.", null);
    }
}
