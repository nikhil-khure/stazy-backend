package com.stazy.backend.complaint.repository;

import com.stazy.backend.common.enums.ComplaintStatus;
import com.stazy.backend.complaint.entity.Complaint;
import com.stazy.backend.user.entity.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintRepository extends JpaRepository<Complaint, UUID> {

    @EntityGraph(attributePaths = {"complainantUser", "againstUser", "relatedListing", "relatedStay"})
    List<Complaint> findByComplainantUserOrderByCreatedAtDesc(User complainantUser);

    @EntityGraph(attributePaths = {"complainantUser", "againstUser", "relatedListing", "relatedStay"})
    List<Complaint> findByAgainstUserOrderByCreatedAtDesc(User againstUser);

    @EntityGraph(attributePaths = {"complainantUser", "againstUser", "relatedListing", "relatedStay"})
    Optional<Complaint> findById(UUID id);

    Optional<Complaint> findByIdAndComplainantUser(UUID id, User complainantUser);

    Optional<Complaint> findByIdAndAgainstUser(UUID id, User againstUser);

    long countByAgainstUserAndStatusIn(User againstUser, Collection<ComplaintStatus> statuses);

    long countByAgainstUserAndStatus(User againstUser, ComplaintStatus status);
}
