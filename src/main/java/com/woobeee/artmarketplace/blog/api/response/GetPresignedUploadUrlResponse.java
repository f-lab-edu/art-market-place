package com.woobeee.artmarketplace.blog.api.response;

public record GetPresignedUploadUrlResponse(
        String uploadUrl,
        String objectKey,
        Long expiresInSeconds
) {
}
