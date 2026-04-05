package com.stazy.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record CityCreateRequest(
        @NotBlank(message = "City name is required.") String cityName,
        @NotBlank(message = "State is required.") String state,
        @NotBlank(message = "Country is required.") String country
) {
}
