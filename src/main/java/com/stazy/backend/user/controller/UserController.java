package com.stazy.backend.user.controller;

import com.stazy.backend.common.api.ApiResponse;
import com.stazy.backend.user.dto.CurrentUserResponse;
import com.stazy.backend.user.dto.DeleteAccountRequest;
import com.stazy.backend.user.dto.UpdatePasswordRequest;
import com.stazy.backend.user.entity.User;
import com.stazy.backend.user.service.CurrentUserService;
import com.stazy.backend.user.service.UserAccountService;
import com.stazy.backend.security.StazyPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final CurrentUserService currentUserService;
    private final UserAccountService userAccountService;

    public UserController(CurrentUserService currentUserService, UserAccountService userAccountService) {
        this.currentUserService = currentUserService;
        this.userAccountService = userAccountService;
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> getCurrentUser(@AuthenticationPrincipal StazyPrincipal principal) {
        User user = currentUserService.requireUser(principal.getUserId());
        return ApiResponse.ok("Current user loaded successfully.", new CurrentUserResponse(
                user.getUserCode(),
                user.getDisplayName(),
                user.getEmail(),
                user.getMobileNumber(),
                user.getPrimaryRoleCode(),
                user.isProfileComplete(),
                user.getCompletionPercentage() == null ? 0 : user.getCompletionPercentage(),
                user.isIdentityVerified(),
                user.getProfilePhotoUrl()
        ));
    }

    @PatchMapping("/me/password")
    public ApiResponse<Void> updatePassword(
            @AuthenticationPrincipal StazyPrincipal principal,
            @Valid @RequestBody UpdatePasswordRequest request
    ) {
        userAccountService.updatePassword(principal.getUserId(), request);
        return ApiResponse.ok("Password updated successfully.");
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> deleteCurrentUser(
            @AuthenticationPrincipal StazyPrincipal principal,
            @Valid @RequestBody DeleteAccountRequest request
    ) {
        userAccountService.deleteAccount(principal.getUserId(), request);
        return ApiResponse.ok("Account marked as deleted.");
    }
}
