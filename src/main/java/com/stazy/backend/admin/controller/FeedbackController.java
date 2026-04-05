package com.stazy.backend.admin.controller;

import com.stazy.backend.admin.dto.FeedbackResponse;
import com.stazy.backend.admin.dto.FeedbackSubmissionRequest;
import com.stazy.backend.admin.service.AdminOperationsService;
import com.stazy.backend.common.api.ApiResponse;
import com.stazy.backend.security.StazyPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedbacks")
public class FeedbackController {

    private final AdminOperationsService adminOperationsService;

    public FeedbackController(AdminOperationsService adminOperationsService) {
        this.adminOperationsService = adminOperationsService;
    }

    @PostMapping("/me")
    public ApiResponse<FeedbackResponse> submitAuthenticatedFeedback(
            @AuthenticationPrincipal StazyPrincipal principal,
            @Valid @RequestBody FeedbackSubmissionRequest request
    ) {
        return ApiResponse.ok("Feedback submitted successfully.", adminOperationsService.submitAuthenticatedFeedback(principal.getUserId(), request));
    }

    @GetMapping
    public ApiResponse<List<FeedbackResponse>> listFeedback(@RequestParam boolean authenticated) {
        return ApiResponse.ok("Feedback loaded successfully.", adminOperationsService.getFeedbackByAuth(authenticated));
    }

    @PatchMapping("/{feedbackId}/publish")
    public ApiResponse<FeedbackResponse> publishFeedback(
            @AuthenticationPrincipal StazyPrincipal principal,
            @PathVariable UUID feedbackId
    ) {
        return ApiResponse.ok("Feedback published successfully.", adminOperationsService.publishFeedback(principal.getUserId(), feedbackId));
    }

    @DeleteMapping("/{feedbackId}")
    public ApiResponse<Void> deleteFeedback(
            @AuthenticationPrincipal StazyPrincipal principal,
            @PathVariable UUID feedbackId
    ) {
        adminOperationsService.deleteFeedback(principal.getUserId(), feedbackId);
        return ApiResponse.ok("Feedback deleted successfully.");
    }
}
