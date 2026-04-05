package com.stazy.backend.listing.dto;

import com.stazy.backend.common.enums.MediaType;

public record ListingMediaResponse(
        String url,
        MediaType mediaType,
        boolean primary
) {
}
