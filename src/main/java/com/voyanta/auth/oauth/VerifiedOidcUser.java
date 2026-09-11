package com.voyanta.auth.oauth;

// Apple çox vaxt "name"-i YALNIZ ilk sign-in-də göndərir (sonrakı girişlərdə yox) —
// ona görə name nullable-dır, çağıran tərəf (OAuthService) buna fallback etməlidir.
public record VerifiedOidcUser(
        String subject,
        String email,
        String name
) {}