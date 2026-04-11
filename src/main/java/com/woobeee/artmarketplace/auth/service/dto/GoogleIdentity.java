package com.woobeee.artmarketplace.auth.service.dto;

public record GoogleIdentity(
        String subject,
        String email,
        String name
) {
}
