package com.woobeee.artmarketplace.auth.api.response;

public record TokenResponse(
        String accessToken,
        long accessTokenExpiresInSeconds,
        String refreshToken,
        long refreshTokenExpiresInSeconds
) {
}
