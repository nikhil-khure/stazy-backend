package com.stazy.backend.booking.repository;

import com.stazy.backend.booking.entity.BookingRequest;
import com.stazy.backend.common.enums.BookingRequestStatus;
import com.stazy.backend.user.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRequestRepository extends JpaRepository<BookingRequest, UUID> {

    List<BookingRequest> findByStudentUserOrderByRequestedAtDesc(User studentUser);

    List<BookingRequest> findByOwnerUserOrderByRequestedAtDesc(User ownerUser);

    Optional<BookingRequest> findByIdAndOwnerUser(UUID id, User ownerUser);

    Optional<BookingRequest> findByIdAndStudentUser(UUID id, User studentUser);

    long countByListingIdAndStatus(UUID listingId, BookingRequestStatus status);
}
