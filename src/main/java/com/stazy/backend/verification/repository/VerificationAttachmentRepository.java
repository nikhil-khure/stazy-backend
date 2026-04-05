package com.stazy.backend.verification.repository;

import com.stazy.backend.verification.entity.VerificationAttachment;
import com.stazy.backend.verification.entity.VerificationRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationAttachmentRepository extends JpaRepository<VerificationAttachment, UUID> {

    List<VerificationAttachment> findByVerificationRequestOrderBySortOrderAsc(VerificationRequest verificationRequest);
}
