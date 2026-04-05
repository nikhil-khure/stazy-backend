package com.stazy.backend.booking.entity;

import com.stazy.backend.common.entity.BaseEntity;
import com.stazy.backend.common.enums.CancelRequestStatus;
import com.stazy.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "stay_cancel_requests")
public class StayCancelRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "active_stay_id", nullable = false)
    private ActiveStay activeStay;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_user_id", nullable = false)
    private User studentUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User ownerUser;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    private String accountStatusSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CancelRequestStatus status = CancelRequestStatus.UNDER_PROGRESS;

    @Column(columnDefinition = "TEXT")
    private String ownerReason;

    @Column(nullable = false)
    private OffsetDateTime requestedAt = OffsetDateTime.now();

    private OffsetDateTime resolvedAt;
}
