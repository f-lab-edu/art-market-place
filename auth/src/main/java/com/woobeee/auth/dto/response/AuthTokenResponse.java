package com.woobeee.auth.dto.response;

public record AuthTokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
