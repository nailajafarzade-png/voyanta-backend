package com.voyanta.auth.dto.response;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserSummary user
) {}