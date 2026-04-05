package com.stazy.backend.verification.repository;

import com.stazy.backend.common.enums.VerificationType;
import com.stazy.backend.listing.entity.Listing;
import com.stazy.backend.user.entity.User;
import com.stazy.backend.verification.entity.VerificationRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationRequestRepository extends JpaRepository<VerificationRequest, UUID> {

    List<VerificationRequest> findByUserOrderByCreatedAtDesc(User user);

    List<VerificationRequest> findByListingOrderByCreatedAtDesc(Listing listing);

    List<VerificationRequest> findByUserAndVerificationTypeOrderByCreatedAtDesc(User user, VerificationType verificationType);
}
