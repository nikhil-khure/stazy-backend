package com.stazy.backend.complaint.entity;

import com.stazy.backend.booking.entity.ActiveStay;
import com.stazy.backend.common.entity.BaseEntity;
import com.stazy.backend.common.enums.ComplaintStatus;
import com.stazy.backend.common.enums.RoleName;
import com.stazy.backend.listing.entity.Listing;
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
@Table(name = "complaints")
public class Complaint extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "complainant_user_id", nullable = false)
    private User complainantUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "against_user_id", nullable = false)
    private User againstUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_listing_id")
    private Listing relatedListing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_stay_id")
    private ActiveStay relatedStay;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "created_by_role_code", nullable = false)
    private RoleName createdByRoleCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "against_role_code", nullable = false)
    private RoleName againstRoleCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplaintStatus status = ComplaintStatus.OPEN;

    @Column(name = "current_resolution_summary", columnDefinition = "TEXT")
    private String currentResolutionSummary;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;
}
