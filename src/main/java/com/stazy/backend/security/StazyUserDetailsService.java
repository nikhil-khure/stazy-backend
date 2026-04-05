package com.stazy.backend.security;

import com.stazy.backend.common.exception.UnauthorizedException;
import com.stazy.backend.user.entity.User;
import com.stazy.backend.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public class StazyUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public StazyUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findWithRolesByEmailIgnoreCase(username)
                .or(() -> userRepository.findWithRolesByUserCodeIgnoreCase(username))
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials."));
        return new StazyPrincipal(user);
    }
}
