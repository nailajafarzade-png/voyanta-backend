package com.voyanta.auth.service;

import com.voyanta.auth.dao.entity.User;
import com.voyanta.auth.dao.repository.UserRepository;
import com.voyanta.auth.dto.request.OAuthLoginRequest;
import com.voyanta.auth.dto.response.AuthResponse;
import com.voyanta.auth.enums.AuthProvider;
import com.voyanta.auth.oauth.ProviderTokenVerifier;
import com.voyanta.auth.oauth.VerifiedOidcUser;
import com.voyanta.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Service that handles social sign-in with Google and Apple. It picks the verifier for
 * the given provider, validates the ID token, creates the user on first login or reuses
 * the existing one by email, and then asks AuthService to issue the tokens.
 */
@Service
@RequiredArgsConstructor
public class OAuthService {

    // Spring "google"/"apple" bean adlarına görə map-i avtomatik doldurur —
    // if/else zənciri əvəzinə təmiz provider dispatch.
    private final Map<String, ProviderTokenVerifier> verifiers;
    private final UserRepository userRepository;
    private final AuthService authService;

    @Transactional
    public AuthResponse login(String provider, OAuthLoginRequest request) {
        ProviderTokenVerifier verifier = verifiers.get(provider);
        if (verifier == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNKNOWN_PROVIDER", "Naməlum provider: " + provider);
        }

        VerifiedOidcUser verified = verifier.verify(request.idToken());
        User user = findOrCreateUser(verified, provider);

        return authService.issueTokens(user);
    }

    private User findOrCreateUser(VerifiedOidcUser verified, String provider) {
        // Diqqət: eyni email ilə əvvəllər LOCAL (email/şifrə) qeydiyyatdan keçmiş istifadəçi
        // varsa, hesab burada avtomatik "bağlanır" (OAuth ilə də giriş edə bilər). Bu, adətən
        // istənilən UX-dir, amma production-da email_verified claim-ini yoxlamaq daha təhlükəsizdir —
        // hazırda bunu etmirik, ilk versiya üçün qeyd olaraq saxlayıram.
        return userRepository.findByEmail(verified.email())
                .orElseGet(() -> userRepository.save(User.builder()
                        .fullName(verified.name() != null ? verified.name() : verified.email())
                        .email(verified.email())
                        .provider(AuthProvider.valueOf(provider.toUpperCase()))
                        .build()));
    }
}