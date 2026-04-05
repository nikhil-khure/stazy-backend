package com.stazy.backend.user.service;

import com.stazy.backend.common.enums.AccountStatus;
import com.stazy.backend.common.exception.BadRequestException;
import com.stazy.backend.common.exception.UnauthorizedException;
import com.stazy.backend.common.util.PasswordRules;
import com.stazy.backend.user.dto.DeleteAccountRequest;
import com.stazy.backend.user.dto.UpdatePasswordRequest;
import com.stazy.backend.user.entity.User;
import com.stazy.backend.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountService {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(
            CurrentUserService currentUserService,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void updatePassword(UUID userId, UpdatePasswordRequest request) {
        User user = currentUserService.requireUser(userId);
        verifyCurrentPassword(user, request.currentPassword());
        if (!Objects.equals(request.newPassword(), request.confirmPassword())) {
            throw new BadRequestException("Passwords do not match.");
        }
        PasswordRules.validate(request.newPassword());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void deleteAccount(UUID userId, DeleteAccountRequest request) {
        User user = currentUserService.requireUser(userId);
        verifyCurrentPassword(user, request.currentPassword());
        user.setDeletedAt(OffsetDateTime.now());
        user.setAccountStatus(AccountStatus.DELETED);
        userRepository.save(user);
    }

    private void verifyCurrentPassword(User user, String rawPassword) {
        if (user.getPasswordHash() == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect.");
        }
    }
}
