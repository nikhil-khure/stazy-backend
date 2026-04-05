package com.stazy.backend.profile.entity;

import com.stazy.backend.common.entity.BaseEntity;
import com.stazy.backend.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "owner_profiles")
public class OwnerProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String panNumber;

    private String pgName;

    private String businessName;

    private String addressLineOne;

    private String addressLineTwo;

    private String locality;

    private String pincode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id")
    private City city;

    private String signatureUrl;

    private String signaturePublicId;

    private String bio;
}
