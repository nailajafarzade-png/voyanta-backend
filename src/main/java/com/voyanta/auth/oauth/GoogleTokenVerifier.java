package com.voyanta.auth.oauth;

import com.voyanta.common.config.VoyantaProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component("google")
@ConditionalOnExpression("'${voyanta.oauth.google.client-id:}'.trim().length() > 0")
class GoogleTokenVerifier implements ProviderTokenVerifier {

    private static final String ISSUER = "https://accounts.google.com";
    private static final String JWKS_URL = "https://www.googleapis.com/oauth2/v3/certs";

    private final OidcTokenVerifier delegate;

    GoogleTokenVerifier(VoyantaProperties properties) {
        this.delegate = new OidcTokenVerifier(ISSUER, JWKS_URL, properties.getOauth().getGoogle().getClientId());
    }

    @Override
    public VerifiedOidcUser verify(String idToken) {
        return delegate.verify(idToken);
    }
}
