package com.stazy.backend.user.entity;

import com.stazy.backend.common.entity.BaseEntity;
import com.stazy.backend.common.enums.AccountStatus;
import com.stazy.backend.common.enums.AuthProvider;
import com.stazy.backend.common.enums.RoleName;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(name = "user_code", nullable = false, unique = true)
    private String userCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "primary_role_code", nullable = false)
    private RoleName primaryRoleCode;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "mobile_number")
    private String mobileNumber;

    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false)
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(name = "is_email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "is_phone_verified", nullable = false)
    private boolean phoneVerified;

    @Column(name = "is_identity_verified", nullable = false)
    private boolean identityVerified;

    @Column(name = "is_profile_complete", nullable = false)
    private boolean profileComplete;

    @Column(name = "completion_percentage", nullable = false)
    private Integer completionPercentage = 0;

    @Column(name = "profile_photo_url")
    private String profilePhotoUrl;

    @Column(name = "profile_photo_public_id")
    private String profilePhotoPublicId;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserRole> userRoles = new LinkedHashSet<>();
}
