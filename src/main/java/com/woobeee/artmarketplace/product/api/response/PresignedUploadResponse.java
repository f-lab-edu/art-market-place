package com.woobeee.artmarketplace.product.api.response;

public record PresignedUploadResponse(
        String uploadUrl,
        String fileKey,
        long expiresInSeconds
) {
}
