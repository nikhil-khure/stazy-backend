package com.stazy.backend.verification.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.stazy.backend.common.entity.BaseEntity;
import com.stazy.backend.common.enums.VerificationStatus;
import com.stazy.backend.common.enums.VerificationType;
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
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "verification_requests")
public class VerificationRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id")
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_user_id")
    private User requestedByUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationType verificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus status = VerificationStatus.PENDING;

    private String aiEndpoint;

    @Column(nullable = false)
    private boolean verified;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_summary_json", columnDefinition = "jsonb")
    private JsonNode requestSummaryJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_response_json", columnDefinition = "jsonb")
    private JsonNode rawResponseJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "accepted_parameters_json", columnDefinition = "jsonb")
    private JsonNode acceptedParametersJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rejected_parameters_json", columnDefinition = "jsonb")
    private JsonNode rejectedParametersJson;

    @Column(columnDefinition = "TEXT")
    private String failureReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    private java.time.OffsetDateTime reviewedAt;
}
