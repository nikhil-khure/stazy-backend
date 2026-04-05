package com.stazy.backend.profile.entity;

import com.stazy.backend.common.entity.BaseEntity;
import com.stazy.backend.common.enums.EmployeeStatus;
import com.stazy.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "admin_profiles")
public class AdminProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private City city;

    private String secretCodeHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "employee_status", nullable = false)
    private EmployeeStatus employeeStatus = EmployeeStatus.ACTIVE;

    @Column(name = "can_manage_all_cities", nullable = false)
    private boolean canManageAllCities;

    private OffsetDateTime lastOtpVerifiedAt;
}
