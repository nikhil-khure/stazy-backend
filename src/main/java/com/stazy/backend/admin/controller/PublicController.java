package com.stazy.backend.admin.controller;

import com.stazy.backend.admin.dto.ContactMessageRequest;
import com.stazy.backend.admin.dto.FeedbackResponse;
import com.stazy.backend.admin.dto.PublicFeedbackRequest;
import com.stazy.backend.admin.service.AdminOperationsService;
import com.stazy.backend.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final AdminOperationsService adminOperationsService;

    public PublicController(AdminOperationsService adminOperationsService) {
        this.adminOperationsService = adminOperationsService;
    }

    @PostMapping("/contact")
    public ApiResponse<Void> submitContact(@Valid @RequestBody ContactMessageRequest request) {
        adminOperationsService.submitContact(request);
        return ApiResponse.ok("Message submitted successfully.");
    }

    @PostMapping("/feedback")
    public ApiResponse<Void> submitPublicFeedback(@Valid @RequestBody PublicFeedbackRequest request) {
        adminOperationsService.submitPublicFeedback(request);
        return ApiResponse.ok("Feedback submitted successfully.");
    }

    @GetMapping("/testimonials")
    public ApiResponse<List<FeedbackResponse>> testimonials() {
        return ApiResponse.ok("Published testimonials loaded successfully.", adminOperationsService.getPublishedFeedback());
    }
}
