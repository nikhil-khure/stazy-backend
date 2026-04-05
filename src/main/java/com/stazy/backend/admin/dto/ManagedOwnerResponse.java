package com.stazy.backend.admin.dto;

import com.stazy.backend.common.enums.AccountStatus;
import com.stazy.backend.common.enums.ListingStatus;
import java.util.UUID;

public record ManagedOwnerResponse(
        UUID userId,
        String userCode,
        String displayName,
        String email,
        String mobileNumber,
        boolean identityVerified,
        AccountStatus accountStatus,
        String listingTitle,
        ListingStatus listingStatus,
        String pgLocation,
        long activeComplaints,
        long resolvedComplaints,
        String panNumber,
        Integer completionPercentage
) {
}
