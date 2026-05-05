package com.woobeee.artmarketplace.product.api.response;

import com.woobeee.artmarketplace.product.entity.ProductStatus;
import java.math.BigDecimal;
import java.util.List;

public record ProductCreateResponse(
        Long productId,
        Long sellerId,
        String height,
        String width,
        String shape,
        String material,
        List<String> tags,
        BigDecimal price,
        ProductStatus status,
        String mainImageKey,
        List<String> thumbnailImageKeys,
        List<String> detailImageKeys
) {
}
