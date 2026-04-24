package com.stazy.backend.booking.repository;

import com.stazy.backend.booking.entity.BookingRequest;
import com.stazy.backend.common.enums.BookingRequestStatus;
import com.stazy.backend.listing.entity.Listing;
import com.stazy.backend.user.entity.User;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRequestRepository extends JpaRepository<BookingRequest, UUID> {

    List<BookingRequest> findByStudentUserOrderByRequestedAtDesc(User studentUser);

    List<BookingRequest> findByOwnerUserOrderByRequestedAtDesc(User ownerUser);

    Optional<BookingRequest> findByIdAndOwnerUser(UUID id, User ownerUser);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BookingRequest b WHERE b.id = :id AND b.ownerUser = :ownerUser")
    Optional<BookingRequest> findByIdAndOwnerUserWithLock(@Param("id") UUID id, @Param("ownerUser") User ownerUser);


    Optional<BookingRequest> findByIdAndStudentUser(UUID id, User studentUser);

    long countByListingIdAndStatus(UUID listingId, BookingRequestStatus status);

    boolean existsByStudentUserAndListingAndStatusIn(User studentUser, Listing listing, List<BookingRequestStatus> statuses);
    
    boolean existsByStudentUserAndStatusIn(User studentUser, List<BookingRequestStatus> statuses);
}
