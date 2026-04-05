package com.stazy.backend.admin.entity;

import com.stazy.backend.common.entity.BaseEntity;
import com.stazy.backend.common.enums.HiringRequestStatus;
import com.stazy.backend.profile.entity.City;
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
@Table(name = "admin_hiring_requests")
public class AdminHiringRequest extends BaseEntity {

    @Column(nullable = false)
    private String fullName;

    private String mobileNumber;

    @Column(nullable = false)
    private String email;

    private String resumeUrl;

    private String resumePublicId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HiringRequestStatus status = HiringRequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_city_id")
    private City assignedCity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    private OffsetDateTime reviewedAt;

    @Column(columnDefinition = "TEXT")
    private String reviewNotes;
}
