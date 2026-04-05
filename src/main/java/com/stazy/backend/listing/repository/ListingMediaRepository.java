package com.stazy.backend.listing.repository;

import com.stazy.backend.listing.entity.Listing;
import com.stazy.backend.listing.entity.ListingMedia;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingMediaRepository extends JpaRepository<ListingMedia, UUID> {

    List<ListingMedia> findByListingOrderBySortOrderAsc(Listing listing);

    void deleteByListing(Listing listing);
}
