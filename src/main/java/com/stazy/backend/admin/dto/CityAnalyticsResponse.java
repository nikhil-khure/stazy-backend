package com.stazy.backend.admin.dto;

public record CityAnalyticsResponse(
        Long cityId,
        String cityName,
        long totalListings,
        long totalOwners,
        long totalStudents
) {
}
