package com.stazy.backend.admin.dto;

public record CityOptionResponse(
        Long cityId,
        String cityName,
        String state,
        String country
) {
}
