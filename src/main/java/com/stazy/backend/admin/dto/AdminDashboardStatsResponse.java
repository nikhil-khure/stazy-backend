package com.stazy.backend.admin.dto;

public record AdminDashboardStatsResponse(
        long totalStudents,
        long totalOwners,
        long liveListings,
        long pendingReviewListings,
        String cityName
) {
}
