package com.stazy.backend.booking.repository;

import com.stazy.backend.booking.entity.ActiveStay;
import com.stazy.backend.booking.entity.RentPayment;
import com.stazy.backend.user.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RentPaymentRepository extends JpaRepository<RentPayment, UUID> {

    List<RentPayment> findByStudentUserOrderByPeriodStartDesc(User studentUser);

    List<RentPayment> findByOwnerUserOrderByPeriodStartDesc(User ownerUser);

    List<RentPayment> findByActiveStayOrderByPeriodStartDesc(ActiveStay activeStay);

    Optional<RentPayment> findByIdAndOwnerUser(UUID id, User ownerUser);
}
