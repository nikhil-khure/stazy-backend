package com.stazy.backend.notification.repository;

import com.stazy.backend.notification.entity.Notification;
import com.stazy.backend.user.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    Optional<Notification> findByIdAndUser(UUID id, User user);
    
    void deleteByUser(User user);
}
