package com.woobeee.auth.dto.provider;

import lombok.Builder;

import java.util.UUID;

@Builder
public record MessageEvent(
        UUID eventId,
        String topic,
        String key,
        Object message
) {
}
