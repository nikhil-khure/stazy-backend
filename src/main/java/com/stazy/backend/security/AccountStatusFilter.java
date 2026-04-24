package com.stazy.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stazy.backend.common.api.ApiResponse;
import com.stazy.backend.common.enums.AccountStatus;
import com.stazy.backend.common.enums.EmployeeStatus;
import com.stazy.backend.common.enums.RoleName;
import com.stazy.backend.profile.entity.AdminProfile;
import com.stazy.backend.profile.repository.AdminProfileRepository;
import com.stazy.backend.user.entity.User;
import com.stazy.backend.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AccountStatusFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final ObjectMapper objectMapper;

    public AccountStatusFilter(
            UserRepository userRepository,
            AdminProfileRepository adminProfileRepository,
            ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.adminProfileRepository = adminProfileRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        // Skip filter for public endpoints
        String path = request.getRequestURI();
        if (isPublicEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() 
                && authentication.getPrincipal() instanceof StazyPrincipal principal) {
            
            UUID userId = principal.getUserId();
            User user = userRepository.findById(userId).orElse(null);
            
            if (user == null) {
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "User not found.");
                return;
            }
            
            // Check if user is deleted
            if (user.getAccountStatus() == AccountStatus.DELETED) {
                sendErrorResponse(response, HttpStatus.FORBIDDEN, "Your account has been deleted.");
                return;
            }
            
            // Check if user is blocked (for students and owners)
            if (user.getAccountStatus() == AccountStatus.BLOCKED) {
                String method = request.getMethod();
                
                // Block all write operations (POST, PUT, PATCH, DELETE)
                if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) 
                        || "PATCH".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)) {
                    String blockReason = user.getBlockReason() == null || user.getBlockReason().isBlank()
                            ? "Your account has been blocked by the admin. You cannot perform this action."
                            : "Your account has been blocked. Reason: " + user.getBlockReason();
                    sendErrorResponse(response, HttpStatus.FORBIDDEN, blockReason);
                    return;
                }
                
                // Allow GET requests (read-only access)
            }
            
            // Check if admin access is revoked
            if (user.getPrimaryRoleCode() == RoleName.ADMIN) {
                AdminProfile adminProfile = adminProfileRepository.findByUser(user).orElse(null);
                if (adminProfile != null && adminProfile.getEmployeeStatus() == EmployeeStatus.REVOKED) {
                    String revokeReason = adminProfile.getRevokeReason() == null || adminProfile.getRevokeReason().isBlank()
                            ? "Your access has been revoked by Super Admin"
                            : adminProfile.getRevokeReason();
                    sendErrorResponse(response, HttpStatus.FORBIDDEN, revokeReason);
                    return;
                }
            }
        }
        
        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/auth/") 
                || path.startsWith("/api/public/")
                || path.startsWith("/ws/")
                || path.equals("/")
                || path.startsWith("/static/")
                || path.startsWith("/assets/");
    }

    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        ApiResponse<Void> apiResponse = ApiResponse.error(message, null);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
