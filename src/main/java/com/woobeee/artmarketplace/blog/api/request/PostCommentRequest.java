package com.woobeee.artmarketplace.blog.api.request;

import lombok.Builder;

@Builder
public record PostCommentRequest(
        Long postId,
        Long parentId,
        String content
) {
}