package com.stazy.backend.complaint.repository;

import com.stazy.backend.complaint.entity.Complaint;
import com.stazy.backend.complaint.entity.ComplaintAttachment;
import com.stazy.backend.complaint.entity.ComplaintMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintAttachmentRepository extends JpaRepository<ComplaintAttachment, UUID> {

    List<ComplaintAttachment> findByComplaintOrderByCreatedAtAsc(Complaint complaint);

    List<ComplaintAttachment> findByComplaintMessageOrderByCreatedAtAsc(ComplaintMessage complaintMessage);
}
