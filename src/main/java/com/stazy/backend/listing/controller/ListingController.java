package com.stazy.backend.listing.controller;

import com.stazy.backend.common.api.ApiResponse;
import com.stazy.backend.common.api.PageResponse;
import com.stazy.backend.listing.dto.ListingResponse;
import com.stazy.backend.listing.dto.ListingUpsertRequest;
import com.stazy.backend.listing.service.ListingService;
import com.stazy.backend.security.StazyPrincipal;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/listings")
public class ListingController {

    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ListingResponse>> searchListings(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String price,
            @RequestParam(required = false) String roomKind,
            @RequestParam(required = false) String genderCategory,
            @RequestParam(required = false) BigDecimal minimumRating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return ApiResponse.ok("Listings loaded successfully.", listingService.search(search, location, price, roomKind, genderCategory, minimumRating, page, size));
    }

    @GetMapping("/{listingId}")
    public ApiResponse<ListingResponse> getListing(@PathVariable UUID listingId) {
        return ApiResponse.ok("Listing loaded successfully.", listingService.getById(listingId));
    }

    @GetMapping("/owner/me")
    public ApiResponse<List<ListingResponse>> getOwnerListings(@AuthenticationPrincipal StazyPrincipal principal) {
        return ApiResponse.ok("Owner listings loaded successfully.", listingService.getOwnerListings(principal.getUserId()));
    }

    @PostMapping("/owner")
    public ApiResponse<ListingResponse> createListing(
            @AuthenticationPrincipal StazyPrincipal principal,
            @Valid @ModelAttribute ListingUpsertRequest request
    ) {
        return ApiResponse.ok("Listing created successfully.", listingService.create(principal.getUserId(), request));
    }

    @PutMapping("/owner/{listingId}")
    public ApiResponse<ListingResponse> updateListing(
            @AuthenticationPrincipal StazyPrincipal principal,
            @PathVariable UUID listingId,
            @Valid @ModelAttribute ListingUpsertRequest request
    ) {
        return ApiResponse.ok("Listing updated successfully.", listingService.update(principal.getUserId(), listingId, request));
    }

    @DeleteMapping("/owner/{listingId}")
    public ApiResponse<Void> deleteListing(
            @AuthenticationPrincipal StazyPrincipal principal,
            @PathVariable UUID listingId
    ) {
        listingService.delete(principal.getUserId(), listingId);
        return ApiResponse.ok("Listing deleted successfully.");
    }
}
