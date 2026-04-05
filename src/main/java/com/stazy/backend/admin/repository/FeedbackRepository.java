package com.stazy.backend.admin.repository;

import com.stazy.backend.admin.entity.Feedback;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {

    List<Feedback> findByIsPublishedTrueOrderByCreatedAtDesc();

    List<Feedback> findByIsAuthenticatedOrderByCreatedAtDesc(boolean isAuthenticated);
}
