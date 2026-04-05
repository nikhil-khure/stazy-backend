package com.stazy.backend.admin.repository;

import com.stazy.backend.admin.entity.AdminHiringRequest;
import com.stazy.backend.common.enums.HiringRequestStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminHiringRequestRepository extends JpaRepository<AdminHiringRequest, UUID> {

    List<AdminHiringRequest> findByStatusOrderByCreatedAtDesc(HiringRequestStatus status);
}
