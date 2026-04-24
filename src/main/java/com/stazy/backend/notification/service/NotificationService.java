package com.stazy.backend.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.stazy.backend.common.enums.NotificationType;
import com.stazy.backend.notification.dto.NotificationResponse;
import com.stazy.backend.notification.entity.Notification;
import com.stazy.backend.notification.repository.NotificationRepository;
import com.stazy.backend.user.entity.User;
import com.stazy.backend.user.service.CurrentUserService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;
    private final com.stazy.backend.common.events.RealtimeEventPublisher realtimeEventPublisher;

    public NotificationService(NotificationRepository notificationRepository, CurrentUserService currentUserService, com.stazy.backend.common.events.RealtimeEventPublisher realtimeEventPublisher) {
        this.notificationRepository = notificationRepository;
        this.currentUserService = currentUserService;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    @Transactional
    public void notifyUser(User user, NotificationType type, String title, String message, String actionPath, JsonNode metadata) {
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setUser(user);
        notification.setNotificationType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setActionPath(actionPath);
        notification.setMetadataJson(metadata);
        notification.setRead(false);
        notification.setCreatedAt(OffsetDateTime.now());
        Notification saved = notificationRepository.save(notification);
        
        realtimeEventPublisher.publishUserEvent(user.getId().toString(), "new_notification", map(saved));
        realtimeEventPublisher.publishGlobalEvent("refresh_needed", new com.stazy.backend.common.events.ServiceActionEvent("NotificationService", "notifyUser"));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> myNotifications(UUID userId) {
        User user = currentUserService.requireUser(userId);
        return notificationRepository.findByUserOrderByCreatedAtDesc(user).stream().map(this::map).toList();
    }

    @Transactional
    public NotificationResponse markRead(UUID userId, UUID notificationId) {
        User user = currentUserService.requireUser(userId);
        Notification notification = notificationRepository.findByIdAndUser(notificationId, user)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found."));
        notification.setRead(true);
        notification.setReadAt(OffsetDateTime.now());
        Notification saved = notificationRepository.save(notification);
        NotificationResponse response = map(saved);
        realtimeEventPublisher.publishUserEvent(user.getId().toString(), "notification_read", response);
        return response;
    }

    @Transactional
    public void clearAllNotifications(UUID userId) {
        User user = currentUserService.requireUser(userId);
        notificationRepository.deleteByUser(user);
        realtimeEventPublisher.publishUserEvent(user.getId().toString(), "notifications_cleared", null);
    }

    private NotificationResponse map(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getNotificationType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getActionPath(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}
