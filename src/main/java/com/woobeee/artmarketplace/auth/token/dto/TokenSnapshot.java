package com.woobeee.artmarketplace.auth.token.dto;

public record TokenSnapshot(
        TokenMetadata metadata,
        long ttlSeconds
) {
}
