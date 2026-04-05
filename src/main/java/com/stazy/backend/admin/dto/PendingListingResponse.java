package com.stazy.backend.admin.dto;

import com.stazy.backend.common.enums.ListingStatus;
import com.stazy.backend.common.enums.VerificationStatus;
import java.util.List;
import java.util.UUID;

public record PendingListingResponse(
        UUID listingId,
        String ownerName,
        String ownerUserCode,
        String ownerPhotoUrl,
        String listingTitle,
        ListingStatus status,
        VerificationStatus fakeDetectionStatus,
        List<String> imageUrls,
        String videoUrl
) {
}
