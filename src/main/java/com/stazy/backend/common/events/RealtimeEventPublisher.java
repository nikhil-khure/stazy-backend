package com.stazy.backend.common.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class RealtimeEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(RealtimeEventPublisher.class);
    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishGlobalEvent(String eventType, Object payload) {
        String destination = "/topic/global";
        if (logger.isTraceEnabled()) {
            logger.trace("Publishing global event {} to {}", eventType, destination);
        }
        messagingTemplate.convertAndSend(destination, new WebSocketEvent(eventType, payload));
    }

    public void publishUserEvent(String userId, String eventType, Object payload) {
        if (userId == null) return;
        String destination = "/topic/user/" + userId;
        if (logger.isTraceEnabled()) {
            logger.trace("Publishing user event {} to {}", eventType, destination);
        }
        messagingTemplate.convertAndSend(destination, new WebSocketEvent(eventType, payload));
    }

    public void publishRoleEvent(String role, String eventType, Object payload) {
        if (role == null) return;
        String destination = "/topic/role/" + role;
        if (logger.isTraceEnabled()) {
            logger.trace("Publishing role event {} to {}", eventType, destination);
        }
        messagingTemplate.convertAndSend(destination, new WebSocketEvent(eventType, payload));
    }

    public void publishEntityEvent(String entityName, String entityId, String eventType, Object payload) {
        String destination = "/topic/entity/" + entityName + "/" + entityId;
        if (logger.isTraceEnabled()) {
            logger.trace("Publishing entity event {} to {}", eventType, destination);
        }
        messagingTemplate.convertAndSend(destination, new WebSocketEvent(eventType, payload));
    }
}
