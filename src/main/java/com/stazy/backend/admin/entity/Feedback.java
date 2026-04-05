package com.stazy.backend.admin.entity;

import com.stazy.backend.common.entity.BaseEntity;
import com.stazy.backend.common.enums.FeedbackScope;
import com.stazy.backend.common.enums.FeedbackVisibilityStatus;
import com.stazy.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "feedbacks")
public class Feedback extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private User targetUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedbackScope feedbackScope;

    private Integer rating;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    private String displayNameSnapshot;

    private String emailSnapshot;

    private String locationSnapshot;

    @Column(nullable = false)
    private boolean isAuthenticated;

    @Column(nullable = false)
    private boolean isPublished;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedbackVisibilityStatus visibilityStatus = FeedbackVisibilityStatus.PENDING_REVIEW;
}
