package com.woobeee.artmarketplace.auth.token.dto;

public record TokenMetadata(
        Long memberId,
        String role,
        String device,
        String ip
) {
}
