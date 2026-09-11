package com.voyanta.auth.oauth;

import com.voyanta.common.config.VoyantaProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component("apple")
@ConditionalOnExpression("'${voyanta.oauth.apple.client-id:}'.trim().length() > 0")
class AppleTokenVerifier implements ProviderTokenVerifier {

    private static final String ISSUER = "https://appleid.apple.com";
    private static final String JWKS_URL = "https://appleid.apple.com/auth/keys";

    private final OidcTokenVerifier delegate;

    AppleTokenVerifier(VoyantaProperties properties) {
        this.delegate = new OidcTokenVerifier(ISSUER, JWKS_URL, properties.getOauth().getApple().getClientId());
    }

    @Override
    public VerifiedOidcUser verify(String idToken) {
        return delegate.verify(idToken);
    }
}
