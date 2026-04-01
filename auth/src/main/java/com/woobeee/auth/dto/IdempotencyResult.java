package com.woobeee.auth.dto;

import lombok.Builder;

@Builder
public record IdempotencyResult(
        boolean proceed,
        boolean inProgress,
        Integer responseCode,
        String responseBody
) {
}
