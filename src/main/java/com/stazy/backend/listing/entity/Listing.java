package com.stazy.backend.listing.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.stazy.backend.common.entity.BaseEntity;
import com.stazy.backend.common.enums.GenderCategory;
import com.stazy.backend.common.enums.ListingStatus;
import com.stazy.backend.common.enums.RoomKind;
import com.stazy.backend.common.enums.VerificationStatus;
import com.stazy.backend.profile.entity.City;
import com.stazy.backend.user.entity.User;
import com.stazy.backend.verification.entity.VerificationRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "listings")
public class Listing extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User ownerUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private City city;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String addressLineOne;

    private String addressLineTwo;

    private String locality;

    private String pincode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomKind roomKind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GenderCategory genderCategory;

    @Column(nullable = false)
    private BigDecimal rentAmount;

    @Column(nullable = false)
    private Integer totalCapacity;

    @Column(nullable = false)
    private Integer availableCapacity;

    @Column(nullable = false)
    private BigDecimal ratingAverage = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer ratingCount = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode amenities;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingStatus status = ListingStatus.UNDER_REVIEW;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus latestFakeDetectionStatus = VerificationStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "latest_verification_request_id")
    private VerificationRequest latestVerificationRequest;

    private OffsetDateTime publishedAt;
}
