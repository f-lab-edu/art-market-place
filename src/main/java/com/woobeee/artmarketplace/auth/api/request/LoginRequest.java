package com.woobeee.artmarketplace.auth.api.request;

import com.woobeee.artmarketplace.auth.entity.MemberType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @NotNull(message = "Member type is required")
        MemberType memberType,
        @NotBlank(message = "Device is required")
        String device
) {
}
