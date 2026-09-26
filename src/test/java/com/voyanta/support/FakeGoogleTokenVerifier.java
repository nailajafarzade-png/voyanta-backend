package com.voyanta.support;

import com.voyanta.auth.oauth.ProviderTokenVerifier;
import com.voyanta.auth.oauth.VerifiedOidcUser;
import com.voyanta.common.exception.ApiException;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Google OIDC verifier used only in tests. Tokens of the form {@code valid:email:name}
 * are accepted; anything else is rejected the same way a bad Google ID token would be.
 */
@Component("google")
@Primary
public class FakeGoogleTokenVerifier implements ProviderTokenVerifier {

    @Override
    public VerifiedOidcUser verify(String idToken) {
        if (idToken != null && idToken.startsWith("valid:")) {
            String[] parts = idToken.split(":", 3);
            String email = parts.length > 1 ? parts[1] : "user@example.com";
            String name = parts.length > 2 ? parts[2] : email;
            return new VerifiedOidcUser("google-sub-" + email, email, name);
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_OAUTH_TOKEN", "OAuth token doğrulanmadı");
    }
}
