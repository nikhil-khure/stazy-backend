package com.stazy.backend.admin.controller;

import com.stazy.backend.admin.dto.AdminDashboardStatsResponse;
import com.stazy.backend.admin.dto.CityCreateRequest;
import com.stazy.backend.admin.dto.CityAnalyticsResponse;
import com.stazy.backend.admin.dto.ConnectedAdminResponse;
import com.stazy.backend.admin.dto.ManagedOwnerResponse;
import com.stazy.backend.admin.dto.ManagedStudentResponse;
import com.stazy.backend.admin.dto.ModeratedUserResponse;
import com.stazy.backend.admin.dto.PendingListingResponse;
import com.stazy.backend.admin.dto.PlatformStatsResponse;
import com.stazy.backend.admin.dto.ReviewNoteRequest;
import com.stazy.backend.admin.dto.UserStatusUpdateRequest;
import com.stazy.backend.admin.service.AdminModerationService;
import com.stazy.backend.common.api.ApiResponse;
import com.stazy.backend.security.StazyPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin")
public class AdminModerationController {

    private final AdminModerationService adminModerationService;

    public AdminModerationController(AdminModerationService adminModerationService) {
        this.adminModerationService = adminModerationService;
    }

    @GetMapping("/dashboard/stats")
    public ApiResponse<AdminDashboardStatsResponse> dashboardStats(@AuthenticationPrincipal StazyPrincipal principal) {
        return ApiResponse.ok("Admin dashboard stats loaded successfully.", adminModerationService.getAdminDashboardStats(principal.getUserId()));
    }

    @GetMapping("/dashboard/students")
    public ApiResponse<List<ManagedStudentResponse>> dashboardStudents(@AuthenticationPrincipal StazyPrincipal principal) {
        return ApiResponse.ok("Managed students loaded successfully.", adminModerationService.getManagedStudents(principal.getUserId()));
    }

    @GetMapping("/dashboard/owners")
    public ApiResponse<List<ManagedOwnerResponse>> dashboardOwners(@AuthenticationPrincipal StazyPrincipal principal) {
        return ApiResponse.ok("Managed owners loaded successfully.", adminModerationService.getManagedOwners(principal.getUserId()));
    }

    @GetMapping("/dashboard/listings/pending")
    public ApiResponse<List<PendingListingResponse>> pendingListings(@AuthenticationPrincipal StazyPrincipal principal) {
        return ApiResponse.ok("Pending listings loaded successfully.", adminModerationService.getPendingListings(principal.getUserId()));
    }

    @PatchMapping("/dashboard/listings/{listingId}/go-live")
    public ApiResponse<PendingListingResponse> goLive(
            @AuthenticationPrincipal StazyPrincipal principal,
            @PathVariable UUID listingId
    ) {
        return ApiResponse.ok("Listing is now live.", adminModerationService.goLive(principal.getUserId(), listingId));
    }

    @PatchMapping("/dashboard/listings/{listingId}/reject")
    public ApiResponse<PendingListingResponse> rejectListing(
            @AuthenticationPrincipal StazyPrincipal principal,
            @PathVariable UUID listingId,
            @Valid @RequestBody ReviewNoteRequest request
    ) {
        return ApiResponse.ok("Listing rejected successfully.", adminModerationService.rejectListing(principal.getUserId(), listingId, request.reviewNotes()));
    }

    @PatchMapping("/dashboard/users/{userId}/status")
    public ApiResponse<ModeratedUserResponse> updateUserStatus(
            @AuthenticationPrincipal StazyPrincipal principal,
            @PathVariable UUID userId,
            @Valid @RequestBody UserStatusUpdateRequest request
    ) {
        return ApiResponse.ok("User status updated successfully.", adminModerationService.updateUserStatus(principal.getUserId(), userId, request.status()));
    }

    @GetMapping("/super/stats")
    public ApiResponse<PlatformStatsResponse> superStats(@AuthenticationPrincipal StazyPrincipal principal) {
        return ApiResponse.ok("Platform stats loaded successfully.", adminModerationService.getPlatformStats(principal.getUserId()));
    }

    @GetMapping("/super/admins")
    public ApiResponse<List<ConnectedAdminResponse>> connectedAdmins(@AuthenticationPrincipal StazyPrincipal principal) {
        return ApiResponse.ok("Connected admins loaded successfully.", adminModerationService.getConnectedAdmins(principal.getUserId()));
    }

    @GetMapping("/super/students")
    public ApiResponse<List<ManagedStudentResponse>> allStudents(@AuthenticationPrincipal StazyPrincipal principal) {
        return ApiResponse.ok("Students loaded successfully.", adminModerationService.getAllStudents(principal.getUserId()));
    }

    @GetMapping("/super/owners")
    public ApiResponse<List<ManagedOwnerResponse>> allOwners(@AuthenticationPrincipal StazyPrincipal principal) {
        return ApiResponse.ok("Owners loaded successfully.", adminModerationService.getAllOwners(principal.getUserId()));
    }

    @GetMapping("/super/cities")
    public ApiResponse<List<CityAnalyticsResponse>> cityAnalytics(@AuthenticationPrincipal StazyPrincipal principal) {
        return ApiResponse.ok("City analytics loaded successfully.", adminModerationService.getCityAnalytics(principal.getUserId()));
    }

    @PostMapping("/super/cities")
    public ApiResponse<CityAnalyticsResponse> createCity(
            @AuthenticationPrincipal StazyPrincipal principal,
            @Valid @RequestBody CityCreateRequest request
    ) {
        return ApiResponse.ok("City created successfully.", adminModerationService.createCity(principal.getUserId(), request));
    }

    @DeleteMapping("/super/users/{userId}")
    public ApiResponse<Void> deleteUser(
            @AuthenticationPrincipal StazyPrincipal principal,
            @PathVariable UUID userId
    ) {
        adminModerationService.deleteUser(principal.getUserId(), userId);
        return ApiResponse.ok("User deleted successfully.");
    }
}
