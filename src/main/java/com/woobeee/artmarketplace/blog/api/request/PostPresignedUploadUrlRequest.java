package com.woobeee.artmarketplace.blog.api.request;

public record PostPresignedUploadUrlRequest(
        String fileName,
        String contentType,
        String folder
) {
}
