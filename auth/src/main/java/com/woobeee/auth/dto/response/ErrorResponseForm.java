package com.woobeee.auth.dto.response;

import lombok.Builder;

@Builder
public record ErrorResponseForm(
        String title,
        int status,
        String timestamp) {
}