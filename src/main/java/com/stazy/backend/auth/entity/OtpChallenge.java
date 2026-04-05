package com.stazy.backend.auth.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.stazy.backend.common.entity.BaseEntity;
import com.stazy.backend.common.enums.OtpChannel;
import com.stazy.backend.common.enums.OtpPurpose;
import com.stazy.backend.common.enums.OtpStatus;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "otp_challenges")
public class OtpChallenge extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String email;

    private String mobileNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OtpPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OtpChannel channel;

    @Column(name = "hashed_code", nullable = false)
    private String hashedCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OtpStatus status = OtpStatus.PENDING;

    @Column(nullable = false)
    private Integer attempts = 0;

    @Column(nullable = false)
    private OffsetDateTime expiresAt;

    private OffsetDateTime consumedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context_json")
    private JsonNode contextJson;
}
