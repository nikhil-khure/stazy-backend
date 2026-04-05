package com.stazy.backend.booking.repository;

import com.stazy.backend.booking.entity.ActiveStay;
import com.stazy.backend.user.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActiveStayRepository extends JpaRepository<ActiveStay, UUID> {

    Optional<ActiveStay> findTopByStudentUserOrderByCreatedAtDesc(User studentUser);

    List<ActiveStay> findByOwnerUserOrderByCreatedAtDesc(User ownerUser);
}
