package com.stazy.backend.booking.repository;

import com.stazy.backend.booking.entity.StayCancelRequest;
import com.stazy.backend.user.entity.User;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StayCancelRequestRepository extends JpaRepository<StayCancelRequest, UUID> {

    List<StayCancelRequest> findByStudentUserOrderByRequestedAtDesc(User studentUser);

    List<StayCancelRequest> findByOwnerUserOrderByRequestedAtDesc(User ownerUser);

    Optional<StayCancelRequest> findByIdAndOwnerUser(UUID id, User ownerUser);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StayCancelRequest s WHERE s.id = :id AND s.ownerUser = :ownerUser")
    Optional<StayCancelRequest> findByIdAndOwnerUserWithLock(@Param("id") UUID id, @Param("ownerUser") User ownerUser);
}
