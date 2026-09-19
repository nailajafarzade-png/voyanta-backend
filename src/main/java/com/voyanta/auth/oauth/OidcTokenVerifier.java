package com.voyanta.auth.oauth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;
import com.voyanta.common.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Shared helper that validates an OIDC ID token against a provider's issuer, JWKS keys
 * and client id, then returns the user claims as a VerifiedOidcUser. Invalid tokens are
 * rejected with an "INVALID_OAUTH_TOKEN" ApiException.
 *
 * Google və Apple hər ikisi OIDC-uyğun provider olduğu üçün ID token doğrulaması
 * eyni məntiqdir — fərqlənən yalnız issuer, JWKS URL-i və client ID-dir.
 * Bu, GoogleTokenVerifier/AppleTokenVerifier tərəfindən konfiqurasiya olunub istifadə edilir.
 */
class OidcTokenVerifier {

    private final IDTokenValidator validator;

    OidcTokenVerifier(String issuerUrl, String jwksUrl, String clientId) {
        try {
            this.validator = new IDTokenValidator(
                    new Issuer(issuerUrl),
                    new ClientID(clientId),
                    JWSAlgorithm.RS256,
                    new URL(jwksUrl)
            );
        } catch (MalformedURLException e) {
            throw new IllegalStateException("JWKS URL yanlışdır: " + jwksUrl, e);
        }
    }

    VerifiedOidcUser verify(String idToken) {
        try {
            SignedJWT jwt = SignedJWT.parse(idToken);
            // nonce yoxlanmır — bu, server-side verifikasiya axınıdır (frontend artıq
            // provider ilə danışıb id_token-i bizə ötürüb), client-side replay riski fərqli qatdadır.
            IDTokenClaimsSet claims = validator.validate(jwt, null);

            String email = claims.getStringClaim("email");
            String name = claims.getStringClaim("name");

            return new VerifiedOidcUser(claims.getSubject().getValue(), email, name);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_OAUTH_TOKEN", "OAuth token doğrulanmadı");
        }
    }
}