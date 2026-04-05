package com.stazy.backend.auth.controller;

import com.stazy.backend.auth.dto.LoginRequest;
import com.stazy.backend.auth.dto.OtpDispatchResponse;
import com.stazy.backend.auth.dto.OtpVerifyRequest;
import com.stazy.backend.auth.dto.OwnerSignupRequest;
import com.stazy.backend.auth.dto.PrivilegedOtpRequest;
import com.stazy.backend.auth.dto.RefreshTokenRequest;
import com.stazy.backend.auth.dto.StudentSignupRequest;
import com.stazy.backend.auth.dto.TokenResponse;
import com.stazy.backend.auth.service.AuthService;
import com.stazy.backend.common.api.ApiResponse;
import com.stazy.backend.common.enums.RoleName;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup/student")
    public ApiResponse<TokenResponse> signupStudent(@Valid @RequestBody StudentSignupRequest request) {
        return ApiResponse.ok("Student account created successfully.", authService.registerStudent(request));
    }

    @PostMapping("/signup/owner")
    public ApiResponse<TokenResponse> signupOwner(@Valid @RequestBody OwnerSignupRequest request) {
        return ApiResponse.ok("Owner account created successfully.", authService.registerOwner(request));
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Login successful.", authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok("Token refreshed successfully.", authService.refresh(request));
    }

    @PostMapping("/admin/send-otp")
    public ApiResponse<OtpDispatchResponse> sendAdminOtp(@Valid @RequestBody PrivilegedOtpRequest request) {
        return ApiResponse.ok("OTP sent successfully.", authService.sendPrivilegedOtp(request, RoleName.ADMIN));
    }

    @PostMapping("/admin/verify-otp")
    public ApiResponse<Void> verifyAdminOtp(@Valid @RequestBody OtpVerifyRequest request) {
        authService.verifyPrivilegedOtp(request, RoleName.ADMIN);
        return ApiResponse.ok("OTP verified successfully.");
    }

    @PostMapping("/admin/login")
    public ApiResponse<TokenResponse> loginAdmin(@Valid @RequestBody PrivilegedOtpRequest request) {
        return ApiResponse.ok("Admin login successful.", authService.loginPrivileged(request, RoleName.ADMIN));
    }

    @PostMapping("/super-admin/send-otp")
    public ApiResponse<OtpDispatchResponse> sendSuperAdminOtp(@Valid @RequestBody PrivilegedOtpRequest request) {
        return ApiResponse.ok("OTP sent successfully.", authService.sendPrivilegedOtp(request, RoleName.SUPER_ADMIN));
    }

    @PostMapping("/super-admin/verify-otp")
    public ApiResponse<Void> verifySuperAdminOtp(@Valid @RequestBody OtpVerifyRequest request) {
        authService.verifyPrivilegedOtp(request, RoleName.SUPER_ADMIN);
        return ApiResponse.ok("OTP verified successfully.");
    }

    @PostMapping("/super-admin/login")
    public ApiResponse<TokenResponse> loginSuperAdmin(@Valid @RequestBody PrivilegedOtpRequest request) {
        return ApiResponse.ok("Super admin login successful.", authService.loginPrivileged(request, RoleName.SUPER_ADMIN));
    }
}
