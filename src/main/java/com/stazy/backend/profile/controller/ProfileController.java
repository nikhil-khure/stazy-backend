package com.stazy.backend.profile.controller;

import com.stazy.backend.common.api.ApiResponse;
import com.stazy.backend.profile.dto.OwnerProfileResponse;
import com.stazy.backend.profile.dto.StudentProfileResponse;
import com.stazy.backend.profile.dto.UpdateOwnerProfileRequest;
import com.stazy.backend.profile.dto.UpdateStudentProfileRequest;
import com.stazy.backend.profile.service.ProfileService;
import com.stazy.backend.security.StazyPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/student/me")
    public ApiResponse<StudentProfileResponse> getStudentProfile(@AuthenticationPrincipal StazyPrincipal principal) {
        return ApiResponse.ok("Student profile loaded successfully.", profileService.getStudentProfile(principal.getUserId()));
    }

    @PutMapping("/student/me")
    public ApiResponse<StudentProfileResponse> updateStudentProfile(
            @AuthenticationPrincipal StazyPrincipal principal,
            @Valid @RequestBody UpdateStudentProfileRequest request
    ) {
        return ApiResponse.ok("Student profile updated successfully.", profileService.updateStudentProfile(principal.getUserId(), request));
    }

    @GetMapping("/owner/me")
    public ApiResponse<OwnerProfileResponse> getOwnerProfile(@AuthenticationPrincipal StazyPrincipal principal) {
        return ApiResponse.ok("Owner profile loaded successfully.", profileService.getOwnerProfile(principal.getUserId()));
    }

    @PutMapping("/owner/me")
    public ApiResponse<OwnerProfileResponse> updateOwnerProfile(
            @AuthenticationPrincipal StazyPrincipal principal,
            @Valid @RequestBody UpdateOwnerProfileRequest request
    ) {
        return ApiResponse.ok("Owner profile updated successfully.", profileService.updateOwnerProfile(principal.getUserId(), request));
    }
}
