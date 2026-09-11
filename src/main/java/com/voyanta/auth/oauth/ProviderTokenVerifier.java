package com.voyanta.auth.oauth;

public interface ProviderTokenVerifier {
    VerifiedOidcUser verify(String idToken);
}