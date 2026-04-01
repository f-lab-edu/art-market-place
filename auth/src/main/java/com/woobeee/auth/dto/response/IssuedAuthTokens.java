package com.woobeee.auth.dto.response;

public record IssuedAuthTokens(
        AuthTokenResponse response,
        String refreshToken
) {
}
