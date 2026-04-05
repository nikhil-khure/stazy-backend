package com.stazy.backend.verification.controller;

import com.stazy.backend.common.api.ApiResponse;
import com.stazy.backend.security.StazyPrincipal;
import com.stazy.backend.verification.dto.OwnerVerificationRequest;
import com.stazy.backend.verification.dto.StudentVerificationRequest;
import com.stazy.backend.verification.dto.VerificationResultResponse;
import com.stazy.backend.verification.service.VerificationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/verifications")
public class VerificationController {

    private final VerificationService verificationService;

    public VerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @PostMapping("/student")
    public ApiResponse<VerificationResultResponse> verifyStudent(
            @AuthenticationPrincipal StazyPrincipal principal,
            @Valid @ModelAttribute StudentVerificationRequest request
    ) {
        return ApiResponse.ok("Student verification completed.", verificationService.verifyStudent(principal.getUserId(), request));
    }

    @PostMapping("/owner")
    public ApiResponse<VerificationResultResponse> verifyOwner(
            @AuthenticationPrincipal StazyPrincipal principal,
            @Valid @ModelAttribute OwnerVerificationRequest request
    ) {
        return ApiResponse.ok("Owner verification completed.", verificationService.verifyOwner(principal.getUserId(), request));
    }

    @PostMapping("/listings/{listingId}")
    public ApiResponse<VerificationResultResponse> verifyListing(
            @AuthenticationPrincipal StazyPrincipal principal,
            @PathVariable UUID listingId
    ) {
        return ApiResponse.ok("Listing verification completed.", verificationService.verifyListing(principal.getUserId(), listingId));
    }

    @GetMapping("/me/history")
    public ApiResponse<List<VerificationResultResponse>> history(@AuthenticationPrincipal StazyPrincipal principal) {
        return ApiResponse.ok("Verification history loaded successfully.", verificationService.historyForCurrentUser(principal.getUserId()));
    }
}
