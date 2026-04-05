package com.stazy.backend.booking.controller;

import com.stazy.backend.booking.dto.ActiveStayResponse;
import com.stazy.backend.booking.dto.BookingCreateRequest;
import com.stazy.backend.booking.dto.BookingRequestResponse;
import com.stazy.backend.booking.dto.BookingReviewRequest;
import com.stazy.backend.booking.dto.CancelRequestCreateRequest;
import com.stazy.backend.booking.dto.CancelRequestReviewRequest;
import com.stazy.backend.booking.dto.RentPaymentResponse;
import com.stazy.backend.booking.dto.RentPaymentUpdateRequest;
import com.stazy.backend.booking.dto.StayCancelResponse;
import com.stazy.backend.booking.service.BookingService;
import com.stazy.backend.common.api.ApiResponse;
import com.stazy.backend.security.StazyPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/listings/{listingId}/requests")
    public ApiResponse<BookingRequestResponse> createRequest(
            @AuthenticationPrincipal StazyPrincipal principal,
            @PathVariable UUID listingId,
            @RequestBody(required = false) BookingCreateRequest request
    ) {
        return ApiResponse.ok("Booking request created successfully.", bookingService.createRequest(principal.getUserId(), listingId, request));
    }

    @GetMapping("/requests/me")
    public ApiResponse<List<BookingRequestResponse>> studentRequests(@AuthenticationPrincipal StazyPrincipal principal) {
        return ApiResponse.ok("Booking requests loaded successfully.", bookingService.studentRequests(principal.getUserId()));
    }

    @GetMapping("/requests/owner")
    public ApiResponse<List<BookingRequestResponse>> ownerRequests(@AuthenticationPrincipal StazyPrincipal principal) {
        return ApiResponse.ok("Owner booking requests loaded successfully.", bookingService.ownerRequests(principal.getUserId()));
    }

    @PatchMapping("/requests/{requestId}/accept")
    public ApiResponse<BookingRequestResponse> acceptRequest(
            @AuthenticationPrincipal StazyPrincipal principal,
            @PathVariable UUID requestId
    ) {
        return ApiResponse.ok("Booking request accepted successfully.", bookingService.acceptRequest(principal.getUserId(), requestId));
    }

    @PatchMapping("/requests/{requestId}/reject")
    public ApiResponse<BookingRequestResponse> rejectRequest(
            @AuthenticationPrincipal StazyPrincipal principal,
            @PathVariable UUID requestId,
            @RequestBody(required = false) BookingReviewRequest request
    ) {
        return ApiResponse.ok("Booking request rejected successfully.", bookingService.rejectRequest(principal.getUserId(), requestId, request));
    }

    @PatchMapping("/requests/{requestId}/revoke")
    public ApiResponse<BookingRequestResponse> revokeRequest(
            @AuthenticationPrincipal StazyPrincipal principal,
            @PathVariable UUID requestId
    ) {
        return ApiResponse.ok("Booking request revoked successfully.", bookingService.revokeRequest(principal.getUserId(), requestId));
    }

    @GetMapping("/active/me")
    public ApiResponse<ActiveStayResponse> currentStay(@AuthenticationPrincipal StazyPrincipal principal) {
        return ApiResponse.ok("Current stay loaded successfully.", bookingService.currentStay(principal.getUserId()));
    }

    @GetMapping("/active/owner")
    public ApiResponse<List<ActiveStayResponse>> ownerStays(@AuthenticationPrincipal StazyPrincipal principal) {
        return ApiResponse.ok("Owner stays loaded successfully.", bookingService.ownerStays(principal.getUserId()));
    }

    @GetMapping("/payments/me")
    public ApiResponse<List<RentPaymentResponse>> payments(@AuthenticationPrincipal StazyPrincipal principal) {
        return ApiResponse.ok("Payments loaded successfully.", bookingService.payments(principal.getUserId()));
    }

    @PatchMapping("/payments/{paymentId}")
    public ApiResponse<RentPaymentResponse> updatePayment(
            @AuthenticationPrincipal StazyPrincipal principal,
            @PathVariable UUID paymentId,
            @Valid @RequestBody RentPaymentUpdateRequest request
    ) {
        return ApiResponse.ok("Payment updated successfully.", bookingService.updatePayment(principal.getUserId(), paymentId, request));
    }

    @PostMapping("/active/{activeStayId}/cancel-requests")
    public ApiResponse<StayCancelResponse> createCancelRequest(
            @AuthenticationPrincipal StazyPrincipal principal,
            @PathVariable UUID activeStayId,
            @Valid @RequestBody CancelRequestCreateRequest request
    ) {
        return ApiResponse.ok("Cancel request submitted successfully.", bookingService.createCancelRequest(principal.getUserId(), activeStayId, request));
    }

    @GetMapping("/cancel-requests/me")
    public ApiResponse<List<StayCancelResponse>> myCancelRequests(@AuthenticationPrincipal StazyPrincipal principal) {
        return ApiResponse.ok("Cancel requests loaded successfully.", bookingService.myCancelRequests(principal.getUserId()));
    }

    @PatchMapping("/cancel-requests/{cancelRequestId}")
    public ApiResponse<StayCancelResponse> reviewCancelRequest(
            @AuthenticationPrincipal StazyPrincipal principal,
            @PathVariable UUID cancelRequestId,
            @RequestBody CancelRequestReviewRequest request
    ) {
        return ApiResponse.ok("Cancel request reviewed successfully.", bookingService.reviewCancelRequest(principal.getUserId(), cancelRequestId, request));
    }
}
