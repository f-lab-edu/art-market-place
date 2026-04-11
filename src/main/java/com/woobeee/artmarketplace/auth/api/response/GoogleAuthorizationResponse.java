package com.woobeee.artmarketplace.auth.api.response;

public record GoogleAuthorizationResponse(
        String authorizationUrl,
        String state,
        long expiresInSeconds
) {
}
