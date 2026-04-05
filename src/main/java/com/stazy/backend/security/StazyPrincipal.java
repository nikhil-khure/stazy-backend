package com.stazy.backend.security;

import com.stazy.backend.user.entity.User;
import java.util.Collection;
import java.util.UUID;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class StazyPrincipal implements UserDetails {

    private final UUID userId;
    private final String userCode;
    private final String password;
    private final String username;
    private final Collection<? extends GrantedAuthority> authorities;

    public StazyPrincipal(User user) {
        this.userId = user.getId();
        this.userCode = user.getUserCode();
        this.password = user.getPasswordHash();
        this.username = user.getEmail();
        this.authorities = user.getUserRoles().stream()
                .map(userRole -> new SimpleGrantedAuthority("ROLE_" + userRole.getRole().getCode().name()))
                .toList();
    }
}
