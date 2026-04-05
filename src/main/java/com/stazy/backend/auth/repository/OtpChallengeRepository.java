package com.stazy.backend.auth.repository;

import com.stazy.backend.auth.entity.OtpChallenge;
import com.stazy.backend.common.enums.OtpPurpose;
import com.stazy.backend.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, UUID> {

    Optional<OtpChallenge> findTopByUserAndPurposeOrderByCreatedAtDesc(User user, OtpPurpose purpose);
}
