package com.stazy.backend.complaint.repository;

import com.stazy.backend.complaint.entity.Complaint;
import com.stazy.backend.complaint.entity.ComplaintMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintMessageRepository extends JpaRepository<ComplaintMessage, UUID> {

    @EntityGraph(attributePaths = {"authorUser"})
    List<ComplaintMessage> findByComplaintOrderByCreatedAtAsc(Complaint complaint);
}
