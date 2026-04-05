package com.stazy.backend.booking.repository;

import com.stazy.backend.booking.entity.StayCancelRequest;
import com.stazy.backend.user.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StayCancelRequestRepository extends JpaRepository<StayCancelRequest, UUID> {

    List<StayCancelRequest> findByStudentUserOrderByRequestedAtDesc(User studentUser);

    List<StayCancelRequest> findByOwnerUserOrderByRequestedAtDesc(User ownerUser);

    Optional<StayCancelRequest> findByIdAndOwnerUser(UUID id, User ownerUser);
}
