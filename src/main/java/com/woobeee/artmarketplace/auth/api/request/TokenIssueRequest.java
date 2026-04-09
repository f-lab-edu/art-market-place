package com.woobeee.artmarketplace.auth.api.request;

public record TokenIssueRequest(
        Long memberId,
        String role,
        String device
) {
}
