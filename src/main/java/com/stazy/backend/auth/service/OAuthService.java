package com.stazy.backend.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.stazy.backend.auth.dto.OAuthLoginRequest;
import com.stazy.backend.auth.dto.TokenResponse;
import com.stazy.backend.auth.entity.OauthAccount;
import com.stazy.backend.auth.repository.OauthAccountRepository;
import com.stazy.backend.common.enums.AuthProvider;
import com.stazy.backend.common.enums.RoleName;
import com.stazy.backend.common.exception.BadRequestException;
import com.stazy.backend.profile.entity.OwnerProfile;
import com.stazy.backend.profile.entity.StudentProfile;
import com.stazy.backend.profile.repository.OwnerProfileRepository;
import com.stazy.backend.profile.repository.StudentProfileRepository;
import com.stazy.backend.profile.service.ProfileCompletionService;
import com.stazy.backend.user.entity.Role;
import com.stazy.backend.user.entity.User;
import com.stazy.backend.user.entity.UserRole;
import com.stazy.backend.user.repository.RoleRepository;
import com.stazy.backend.user.repository.UserRepository;
import com.stazy.backend.user.service.UserCodeGenerator;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Service
public class OAuthService {

    private final OauthAccountRepository oauthAccountRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final UserCodeGenerator userCodeGenerator;
    private final ProfileCompletionService profileCompletionService;
    private final AuthService authService;
    private final RestClient restClient;

    public OAuthService(
            OauthAccountRepository oauthAccountRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            StudentProfileRepository studentProfileRepository,
            OwnerProfileRepository ownerProfileRepository,
            UserCodeGenerator userCodeGenerator,
            ProfileCompletionService profileCompletionService,
            AuthService authService
    ) {
        this.oauthAccountRepository = oauthAccountRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.ownerProfileRepository = ownerProfileRepository;
        this.userCodeGenerator = userCodeGenerator;
        this.profileCompletionService = profileCompletionService;
        this.authService = authService;
        this.restClient = RestClient.builder().build();
    }

    @Transactional
    public TokenResponse authenticate(OAuthLoginRequest request) {
        if (request.role() != RoleName.STUDENT && request.role() != RoleName.OWNER) {
            throw new BadRequestException("OAuth is available only for student and owner accounts.");
        }
        OAuthIdentity identity = switch (request.provider()) {
            case GOOGLE -> verifyGoogleCredential(request.credential());
            case FACEBOOK -> verifyFacebookCredential(request.credential());
            default -> throw new BadRequestException("Unsupported OAuth provider.");
        };

        User user = oauthAccountRepository.findByProviderAndProviderSubject(request.provider(), identity.subject())
                .map(OauthAccount::getUser)
                .orElseGet(() -> resolveOrCreateUser(request, identity));

        if (user.getPrimaryRoleCode() != request.role()) {
            throw new BadRequestException("This account is registered as " + user.getPrimaryRoleCode().name() + ".");
        }
        return authService.issueTokensForUser(userRepository.save(user));
    }

    private User resolveOrCreateUser(OAuthLoginRequest request, OAuthIdentity identity) {
        User existingUser = identity.email() == null ? null : userRepository.findByEmailIgnoreCase(identity.email()).orElse(null);
        if (existingUser != null) {
            linkProvider(existingUser, request.provider(), identity);
            return existingUser;
        }
        if ("LOGIN".equalsIgnoreCase(request.mode())) {
            throw new BadRequestException("No account found. Please sign up first.");
        }
        return createUser(request.role(), request.provider(), identity);
    }

    private User createUser(RoleName roleName, AuthProvider provider, OAuthIdentity identity) {
        Role role = roleRepository.findByCode(roleName)
                .orElseThrow(() -> new BadRequestException("Role " + roleName.name() + " is not configured."));

        User user = new User();
        user.setPrimaryRoleCode(roleName);
        user.setDisplayName(identity.displayName());
        user.setEmail(identity.email());
        user.setAuthProvider(provider);
        user.setEmailVerified(identity.email() != null);
        user.setUserCode(userCodeGenerator.generate(roleName, identity.displayName(), userRepository.count() + 1));

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        user.getUserRoles().add(userRole);
        user = userRepository.save(user);

        if (roleName == RoleName.STUDENT) {
            StudentProfile profile = new StudentProfile();
            profile.setUser(user);
            studentProfileRepository.save(profile);
            profileCompletionService.refreshStudentCompletion(user, profile);
        } else {
            OwnerProfile profile = new OwnerProfile();
            profile.setUser(user);
            ownerProfileRepository.save(profile);
            profileCompletionService.refreshOwnerCompletion(user, profile);
        }
        user = userRepository.save(user);
        linkProvider(user, provider, identity);
        return user;
    }

    private void linkProvider(User user, AuthProvider provider, OAuthIdentity identity) {
        oauthAccountRepository.findByProviderAndProviderSubject(provider, identity.subject()).orElseGet(() -> {
            OauthAccount oauthAccount = new OauthAccount();
            oauthAccount.setUser(user);
            oauthAccount.setProvider(provider);
            oauthAccount.setProviderSubject(identity.subject());
            oauthAccount.setEmail(identity.email());
            return oauthAccountRepository.save(oauthAccount);
        });
    }

    private OAuthIdentity verifyGoogleCredential(String credential) {
        JsonNode payload = restClient.get()
                .uri("https://oauth2.googleapis.com/tokeninfo?id_token={token}", credential)
                .retrieve()
                .body(JsonNode.class);
        if (payload == null || payload.path("sub").asText("").isBlank()) {
            throw new BadRequestException("Unable to verify Google credential.");
        }
        return new OAuthIdentity(
                payload.path("sub").asText(),
                payload.path("email").asText(null),
                payload.path("name").asText(payload.path("email").asText("Google User"))
        );
    }

    private OAuthIdentity verifyFacebookCredential(String credential) {
        JsonNode payload = restClient.get()
                .uri("https://graph.facebook.com/me?fields=id,name,email&access_token={token}", credential)
                .retrieve()
                .body(JsonNode.class);
        if (payload == null || payload.path("id").asText("").isBlank()) {
            throw new BadRequestException("Unable to verify Facebook credential.");
        }
        String email = payload.path("email").asText(null);
        return new OAuthIdentity(
                payload.path("id").asText(),
                email == null ? null : email.toLowerCase(Locale.ROOT),
                payload.path("name").asText(email == null ? "Facebook User" : email)
        );
    }

    private record OAuthIdentity(String subject, String email, String displayName) {
    }
}
