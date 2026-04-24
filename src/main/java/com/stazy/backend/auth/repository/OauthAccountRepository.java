package com.stazy.backend.auth.repository;

import com.stazy.backend.auth.entity.OauthAccount;
import com.stazy.backend.common.enums.AuthProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OauthAccountRepository extends JpaRepository<OauthAccount, UUID> {

    Optional<OauthAccount> findByProviderAndProviderSubject(AuthProvider provider, String providerSubject);
}
