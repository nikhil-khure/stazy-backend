package com.stazy.backend.user.service;

import com.stazy.backend.common.enums.AccountStatus;
import com.stazy.backend.common.exception.UnauthorizedException;
import com.stazy.backend.common.exception.NotFoundException;
import com.stazy.backend.user.entity.User;
import com.stazy.backend.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User requireUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found."));
        if (user.getAccountStatus() == AccountStatus.DELETED) {
            throw new NotFoundException("User not found.");
        }
        if (user.getAccountStatus() == AccountStatus.BLOCKED || user.getAccountStatus() == AccountStatus.SUSPENDED) {
            throw new UnauthorizedException("Your account is blocked. Please contact support.");
        }
        return user;
    }
}
