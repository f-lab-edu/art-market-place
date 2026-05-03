package com.woobeee.artmarketplace.product.api.response;

import com.woobeee.artmarketplace.product.entity.ProductStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductListResponse(
        boolean hasNext,
        List<ProductSummary> contents
) {
    public record ProductSummary(
            Long productId,
            Long sellerId,
            String artist,
            String height,
            String width,
            String shape,
            String material,
            List<String> tags,
            BigDecimal price,
            ProductStatus status,
            String mainImageKey,
            List<String> detailImageKeys,
            LocalDateTime createdAt
    ) {
    }
}
