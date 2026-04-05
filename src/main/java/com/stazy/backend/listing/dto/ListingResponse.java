package com.stazy.backend.listing.dto;

import com.stazy.backend.common.enums.GenderCategory;
import com.stazy.backend.common.enums.ListingStatus;
import com.stazy.backend.common.enums.RoomKind;
import com.stazy.backend.common.enums.VerificationStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ListingResponse(
        UUID id,
        String title,
        String description,
        String location,
        BigDecimal rentAmount,
        BigDecimal ratingAverage,
        int ratingCount,
        RoomKind roomKind,
        GenderCategory genderCategory,
        int totalCapacity,
        int availableCapacity,
        ListingStatus status,
        VerificationStatus latestFakeDetectionStatus,
        boolean verified,
        List<String> amenities,
        List<ListingMediaResponse> media
) {
}
