package com.stazy.backend.admin.controller;

import com.stazy.backend.admin.dto.AdminHiringRequestCreate;
import com.stazy.backend.admin.dto.AdminHiringRequestResponse;
import com.stazy.backend.admin.dto.AdminQueryCreateRequest;
import com.stazy.backend.admin.dto.AdminQueryReplyRequest;
import com.stazy.backend.admin.dto.AdminQueryResponse;
import com.stazy.backend.admin.dto.HireAdminRequest;
import com.stazy.backend.admin.dto.ReviewNoteRequest;
import com.stazy.backend.admin.service.AdminOperationsService;
import com.stazy.backend.common.api.ApiResponse;
import com.stazy.backend.common.enums.HiringRequestStatus;
import com.stazy.backend.integration.cloudinary.DownloadedAsset;
import com.stazy.backend.security.StazyPrincipal;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin")
public class AdminOperationsController {

    private final AdminOperationsService adminOperationsService;

    public AdminOperationsController(AdminOperationsService adminOperationsService) {
        this.adminOperationsService = adminOperationsService;
    }

    @PostMapping("/hiring-requests")
    public ApiResponse<AdminHiringRequestResponse> createHiringRequest(@Valid @ModelAttribute AdminHiringRequestCreate request) {
        return ApiResponse.ok("Hiring request submitted successfully.", adminOperationsService.submitHiringRequest(request));
    }

    @GetMapping("/queries/me")
    public ApiResponse<List<AdminQueryResponse>> myQueries(@AuthenticationPrincipal StazyPrincipal principal) {
        return ApiResponse.ok("Admin queries loaded successfully.", adminOperationsService.getAdminQueries(principal.getUserId()));
    }

    @PostMapping("/queries")
    public ApiResponse<AdminQueryResponse> createQuery(
            @AuthenticationPrincipal StazyPrincipal principal,
            @Valid @RequestBody AdminQueryCreateRequest request
    ) {
        return ApiResponse.ok("Admin query submitted successfully.", adminOperationsService.createAdminQuery(principal.getUserId(), request));
    }

    @GetMapping("/super/hiring-requests")
    public ApiResponse<List<AdminHiringRequestResponse>> hiringRequests(@RequestParam(defaultValue = "PENDING") HiringRequestStatus status) {
        return ApiResponse.ok("Hiring requests loaded successfully.", adminOperationsService.getHiringRequests(status));
    }

    @GetMapping("/super/hiring-requests/{requestId}/resume")
    public ResponseEntity<ByteArrayResource> hiringRequestResume(
            @AuthenticationPrincipal StazyPrincipal principal,
            @PathVariable UUID requestId
    ) {
        DownloadedAsset asset = adminOperationsService.getHiringRequestResume(principal.getUserId(), requestId);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(asset.fileName(), StandardCharsets.UTF_8).build().toString()
                )
                .contentType(asset.mediaType())
                .contentLength(asset.bytes().length)
                .body(new ByteArrayResource(asset.bytes()));
    }

    @PatchMapping("/super/hiring-requests/{requestId}/hire")
    public ApiResponse<AdminHiringRequestResponse> hireAdmin(
            @AuthenticationPrincipal StazyPrincipal principal,
            @PathVariable UUID requestId,
            @Valid @RequestBody HireAdminRequest request
    ) {
        return ApiResponse.ok("Admin hired successfully.", adminOperationsService.hireAdmin(principal.getUserId(), requestId, request));
    }

    @PatchMapping("/super/hiring-requests/{requestId}/reject")
    public ApiResponse<AdminHiringRequestResponse> rejectAdmin(
            @AuthenticationPrincipal StazyPrincipal principal,
            @PathVariable UUID requestId,
            @Valid @RequestBody ReviewNoteRequest request
    ) {
        return ApiResponse.ok("Hiring request rejected successfully.", adminOperationsService.rejectHiringRequest(principal.getUserId(), requestId, request.reviewNotes()));
    }

    @GetMapping("/super/queries")
    public ApiResponse<List<AdminQueryResponse>> allQueries(@AuthenticationPrincipal StazyPrincipal principal) {
        return ApiResponse.ok("Admin queries loaded successfully.", adminOperationsService.getAllQueries(principal.getUserId()));
    }

    @PatchMapping("/super/queries/{queryId}/reply")
    public ApiResponse<AdminQueryResponse> replyToQuery(
            @AuthenticationPrincipal StazyPrincipal principal,
            @PathVariable UUID queryId,
            @Valid @RequestBody AdminQueryReplyRequest request
    ) {
        return ApiResponse.ok("Reply sent successfully.", adminOperationsService.replyToQuery(principal.getUserId(), queryId, request));
    }
}
