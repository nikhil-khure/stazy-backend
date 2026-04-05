package com.stazy.backend.complaint.controller;

import com.stazy.backend.common.api.ApiResponse;
import com.stazy.backend.complaint.dto.ComplaintActionRequest;
import com.stazy.backend.complaint.dto.ComplaintCreateRequest;
import com.stazy.backend.complaint.dto.ComplaintResponse;
import com.stazy.backend.complaint.service.ComplaintService;
import com.stazy.backend.security.StazyPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @PostMapping
    public ApiResponse<ComplaintResponse> createComplaint(
            @AuthenticationPrincipal StazyPrincipal principal,
            @Valid @ModelAttribute ComplaintCreateRequest request
    ) {
        return ApiResponse.ok("Complaint submitted successfully.", complaintService.createComplaint(principal.getUserId(), request));
    }

    @GetMapping("/filed")
    public ApiResponse<List<ComplaintResponse>> filedComplaints(@AuthenticationPrincipal StazyPrincipal principal) {
        return ApiResponse.ok("Filed complaints loaded successfully.", complaintService.filedComplaints(principal.getUserId()));
    }

    @GetMapping("/received")
    public ApiResponse<List<ComplaintResponse>> receivedComplaints(@AuthenticationPrincipal StazyPrincipal principal) {
        return ApiResponse.ok("Received complaints loaded successfully.", complaintService.receivedComplaints(principal.getUserId()));
    }

    @GetMapping("/{complaintId}")
    public ApiResponse<ComplaintResponse> getComplaint(
            @AuthenticationPrincipal StazyPrincipal principal,
            @PathVariable UUID complaintId
    ) {
        return ApiResponse.ok("Complaint loaded successfully.", complaintService.getComplaint(principal.getUserId(), complaintId));
    }

    @PostMapping("/{complaintId}/justify")
    public ApiResponse<ComplaintResponse> justifyComplaint(
            @AuthenticationPrincipal StazyPrincipal principal,
            @PathVariable UUID complaintId,
            @Valid @ModelAttribute ComplaintActionRequest request
    ) {
        return ApiResponse.ok("Complaint justification submitted successfully.", complaintService.submitJustification(principal.getUserId(), complaintId, request));
    }

    @PostMapping("/{complaintId}/re-open")
    public ApiResponse<ComplaintResponse> reOpenComplaint(
            @AuthenticationPrincipal StazyPrincipal principal,
            @PathVariable UUID complaintId,
            @Valid @ModelAttribute ComplaintActionRequest request
    ) {
        return ApiResponse.ok("Complaint reopened successfully.", complaintService.reComplaint(principal.getUserId(), complaintId, request));
    }

    @PatchMapping("/{complaintId}/close")
    public ApiResponse<ComplaintResponse> closeComplaint(
            @AuthenticationPrincipal StazyPrincipal principal,
            @PathVariable UUID complaintId
    ) {
        return ApiResponse.ok("Complaint closed successfully.", complaintService.closeComplaint(principal.getUserId(), complaintId));
    }
}
