package com.woobeee.artmarketplace.auth.api.request;

public record TokenRefreshRequest(
        String refreshToken,
        String device
) {
}
