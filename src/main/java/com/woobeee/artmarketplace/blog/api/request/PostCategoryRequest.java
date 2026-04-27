package com.woobeee.artmarketplace.blog.api.request;

import lombok.Builder;

@Builder
public record PostCategoryRequest(
        String nameKo,
        String nameEn
) {
}