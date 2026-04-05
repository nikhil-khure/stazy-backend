package com.stazy.backend.auth.service;

import com.stazy.backend.auth.dto.AuthUserResponse;
import com.stazy.backend.auth.dto.LoginRequest;
import com.stazy.backend.auth.dto.OtpDispatchResponse;
import com.stazy.backend.auth.dto.OtpVerifyRequest;
import com.stazy.backend.auth.dto.OwnerSignupRequest;
import com.stazy.backend.auth.dto.PrivilegedOtpRequest;
import com.stazy.backend.auth.dto.RefreshTokenRequest;
import com.stazy.backend.auth.dto.StudentSignupRequest;
import com.stazy.backend.auth.dto.TokenResponse;
import com.stazy.backend.auth.entity.OtpChallenge;
import com.stazy.backend.auth.entity.RefreshToken;
import com.stazy.backend.auth.repository.OtpChallengeRepository;
import com.stazy.backend.auth.repository.RefreshTokenRepository;
import com.stazy.backend.common.config.AppProperties;
import com.stazy.backend.common.enums.AuthProvider;
import com.stazy.backend.common.enums.OtpChannel;
import com.stazy.backend.common.enums.OtpPurpose;
import com.stazy.backend.common.enums.OtpStatus;
import com.stazy.backend.common.enums.RoleName;
import com.stazy.backend.common.exception.BadRequestException;
import com.stazy.backend.common.exception.UnauthorizedException;
import com.stazy.backend.common.util.PasswordRules;
import com.stazy.backend.profile.entity.AdminProfile;
import com.stazy.backend.profile.entity.OwnerProfile;
import com.stazy.backend.profile.entity.StudentProfile;
import com.stazy.backend.profile.repository.AdminProfileRepository;
import com.stazy.backend.profile.repository.OwnerProfileRepository;
import com.stazy.backend.profile.repository.StudentProfileRepository;
import com.stazy.backend.profile.service.ProfileCompletionService;
import com.stazy.backend.security.JwtService;
import com.stazy.backend.security.StazyPrincipal;
import com.stazy.backend.user.entity.Role;
import com.stazy.backend.user.entity.User;
import com.stazy.backend.user.entity.UserRole;
import com.stazy.backend.user.repository.RoleRepository;
import com.stazy.backend.user.repository.UserRepository;
import com.stazy.backend.user.service.UserCodeGenerator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpChallengeRepository otpChallengeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserCodeGenerator userCodeGenerator;
    private final ProfileCompletionService profileCompletionService;
    private final AppProperties appProperties;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            StudentProfileRepository studentProfileRepository,
            OwnerProfileRepository ownerProfileRepository,
            AdminProfileRepository adminProfileRepository,
            RefreshTokenRepository refreshTokenRepository,
            OtpChallengeRepository otpChallengeRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            UserCodeGenerator userCodeGenerator,
            ProfileCompletionService profileCompletionService,
            AppProperties appProperties
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.ownerProfileRepository = ownerProfileRepository;
        this.adminProfileRepository = adminProfileRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.otpChallengeRepository = otpChallengeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userCodeGenerator = userCodeGenerator;
        this.profileCompletionService = profileCompletionService;
        this.appProperties = appProperties;
    }

    @Transactional
    public TokenResponse registerStudent(StudentSignupRequest request) {
        validatePasswords(request.password(), request.confirmPassword());
        User user = createBaseUser(request.name(), request.email(), request.mobile(), request.password(), RoleName.STUDENT);
        StudentProfile profile = new StudentProfile();
        profile.setUser(user);
        profile.setCollegeName(blankToNull(request.collegeName()));
        profileCompletionService.refreshStudentCompletion(user, profile);
        userRepository.save(user);
        studentProfileRepository.save(profile);
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse registerOwner(OwnerSignupRequest request) {
        validatePasswords(request.password(), request.confirmPassword());
        User user = createBaseUser(request.name(), request.email(), request.mobile(), request.password(), RoleName.OWNER);
        OwnerProfile profile = new OwnerProfile();
        profile.setUser(user);
        profileCompletionService.refreshOwnerCompletion(user, profile);
        userRepository.save(user);
        ownerProfileRepository.save(profile);
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = findUserByLoginId(request.loginId());
        if (!Objects.equals(user.getPrimaryRoleCode(), request.role())) {
            throw new UnauthorizedException("This account is not allowed to log in as " + request.role().name() + ".");
        }
        verifyPassword(request.password(), user.getPasswordHash());
        user.setLastLoginAt(OffsetDateTime.now());
        return issueTokens(userRepository.save(user));
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        String tokenHash = sha256(request.refreshToken());
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token."));
        if (storedToken.getRevokedAt() != null || storedToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new UnauthorizedException("Refresh token has expired.");
        }
        User user = storedToken.getUser();
        storedToken.setRevokedAt(OffsetDateTime.now());
        refreshTokenRepository.save(storedToken);
        return issueTokens(user);
    }

    @Transactional
    public OtpDispatchResponse sendPrivilegedOtp(PrivilegedOtpRequest request, RoleName roleName) {
        User user = findPrivilegedUser(request.loginId(), roleName);
        verifyPassword(request.password(), user.getPasswordHash());
        AdminProfile adminProfile = adminProfileRepository.findByUser(user)
                .orElseThrow(() -> new UnauthorizedException("Admin profile not found."));
        verifyPassword(request.secretCode(), adminProfile.getSecretCodeHash());

        String otp = generateOtp();
        OtpChallenge challenge = new OtpChallenge();
        challenge.setUser(user);
        challenge.setEmail(user.getEmail());
        challenge.setChannel(OtpChannel.EMAIL);
        challenge.setPurpose(roleName == RoleName.SUPER_ADMIN ? OtpPurpose.SUPER_ADMIN_LOGIN : OtpPurpose.ADMIN_LOGIN);
        challenge.setHashedCode(passwordEncoder.encode(otp));
        challenge.setExpiresAt(OffsetDateTime.now().plusMinutes(appProperties.getOtp().getExpiryMinutes()));
        challenge.setStatus(OtpStatus.PENDING);
        otpChallengeRepository.save(challenge);

        log.info("Generated {} OTP for {}: {}", roleName, user.getEmail(), otp);
        return new OtpDispatchResponse(maskEmail(user.getEmail()), appProperties.getOtp().isRevealInResponse() ? otp : null);
    }

    @Transactional
    public void verifyPrivilegedOtp(OtpVerifyRequest request, RoleName roleName) {
        User user = findPrivilegedUser(request.loginId(), roleName);
        OtpChallenge challenge = getLatestChallenge(user, roleName);
        if (challenge.getExpiresAt().isBefore(OffsetDateTime.now())) {
            challenge.setStatus(OtpStatus.EXPIRED);
            throw new BadRequestException("OTP has expired. Please request a new OTP.");
        }
        if (!passwordEncoder.matches(request.otp(), challenge.getHashedCode())) {
            challenge.setAttempts(challenge.getAttempts() + 1);
            if (challenge.getAttempts() >= appProperties.getOtp().getMaxAttempts()) {
                challenge.setStatus(OtpStatus.FAILED);
            }
            throw new BadRequestException("Invalid OTP.");
        }
        challenge.setStatus(OtpStatus.VERIFIED);
        otpChallengeRepository.save(challenge);
    }

    @Transactional
    public TokenResponse loginPrivileged(PrivilegedOtpRequest request, RoleName roleName) {
        User user = findPrivilegedUser(request.loginId(), roleName);
        verifyPassword(request.password(), user.getPasswordHash());
        AdminProfile adminProfile = adminProfileRepository.findByUser(user)
                .orElseThrow(() -> new UnauthorizedException("Admin profile not found."));
        verifyPassword(request.secretCode(), adminProfile.getSecretCodeHash());

        OtpChallenge challenge = getLatestChallenge(user, roleName);
        if (challenge.getStatus() != OtpStatus.VERIFIED) {
            throw new UnauthorizedException("Please verify OTP before logging in.");
        }
        if (challenge.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new UnauthorizedException("OTP has expired. Please request a new OTP.");
        }
        challenge.setConsumedAt(OffsetDateTime.now());
        adminProfile.setLastOtpVerifiedAt(OffsetDateTime.now());
        user.setLastLoginAt(OffsetDateTime.now());
        return issueTokens(userRepository.save(user));
    }

    private User createBaseUser(String name, String email, String mobile, String password, RoleName roleName) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("An account with this email already exists.");
        }
        PasswordRules.validate(password);

        User user = new User();
        user.setDisplayName(name.trim());
        user.setEmail(email.trim().toLowerCase());
        user.setMobileNumber(blankToNull(mobile));
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setPrimaryRoleCode(roleName);
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setUserCode(userCodeGenerator.generate(roleName, name, userRepository.count() + 1));

        Role role = roleRepository.findByCode(roleName)
                .orElseThrow(() -> new BadRequestException("Role " + roleName + " is not configured."));
        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        user.getUserRoles().add(userRole);
        return user;
    }

    private void validatePasswords(String password, String confirmPassword) {
        if (!Objects.equals(password, confirmPassword)) {
            throw new BadRequestException("Passwords do not match.");
        }
        PasswordRules.validate(password);
    }

    private User findUserByLoginId(String loginId) {
        return userRepository.findWithRolesByUserCodeIgnoreCase(loginId)
                .or(() -> userRepository.findWithRolesByEmailIgnoreCase(loginId))
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials."));
    }

    private User findPrivilegedUser(String loginId, RoleName roleName) {
        User user = findUserByLoginId(loginId);
        if (user.getPrimaryRoleCode() != roleName) {
            throw new UnauthorizedException("This account is not allowed to log in as " + roleName.name() + ".");
        }
        return user;
    }

    private OtpChallenge getLatestChallenge(User user, RoleName roleName) {
        OtpPurpose purpose = roleName == RoleName.SUPER_ADMIN ? OtpPurpose.SUPER_ADMIN_LOGIN : OtpPurpose.ADMIN_LOGIN;
        return otpChallengeRepository.findTopByUserAndPurposeOrderByCreatedAtDesc(user, purpose)
                .orElseThrow(() -> new BadRequestException("OTP has not been requested yet."));
    }

    private void verifyPassword(String rawPassword, String encodedPassword) {
        if (encodedPassword == null || !passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new UnauthorizedException("Invalid credentials.");
        }
    }

    private TokenResponse issueTokens(User user) {
        StazyPrincipal principal = new StazyPrincipal(user);
        String accessToken = jwtService.generateAccessToken(principal);
        UUID refreshTokenId = UUID.randomUUID();
        String refreshTokenValue = jwtService.generateRefreshToken(principal, refreshTokenId);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(sha256(refreshTokenValue));
        refreshToken.setDeviceName("web");
        refreshToken.setExpiresAt(OffsetDateTime.now().plusDays(appProperties.getJwt().getRefreshTokenExpiryDays()));
        refreshTokenRepository.save(refreshToken);

        return new TokenResponse(accessToken, refreshTokenValue, buildUserResponse(user));
    }

    private AuthUserResponse buildUserResponse(User user) {
        return new AuthUserResponse(
                user.getId(),
                user.getUserCode(),
                user.getPrimaryRoleCode(),
                user.getDisplayName(),
                user.getEmail(),
                user.isProfileComplete(),
                user.getCompletionPercentage() == null ? 0 : user.getCompletionPercentage(),
                user.isIdentityVerified()
        );
    }

    private String generateOtp() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }

    private String sha256(String rawValue) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawValue.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to hash value.", ex);
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "registered email";
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String maskedLocal = local.length() <= 2 ? "*".repeat(local.length()) : local.substring(0, 2) + "*".repeat(local.length() - 2);
        return maskedLocal + "@" + parts[1];
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
