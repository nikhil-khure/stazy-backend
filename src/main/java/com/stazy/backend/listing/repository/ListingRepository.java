package com.stazy.backend.listing.repository;

import com.stazy.backend.common.enums.ListingStatus;
import com.stazy.backend.listing.entity.Listing;
import com.stazy.backend.user.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ListingRepository extends JpaRepository<Listing, UUID>, JpaSpecificationExecutor<Listing> {

    List<Listing> findByOwnerUserOrderByCreatedAtDesc(User ownerUser);

    List<Listing> findByStatusOrderByCreatedAtDesc(ListingStatus status);

    Optional<Listing> findByIdAndOwnerUser(UUID id, User ownerUser);

    long countByStatus(ListingStatus status);
}
