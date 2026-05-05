package com.woobeee.artmarketplace.product.api.response;

import com.woobeee.artmarketplace.product.entity.ProductStatus;
import java.util.List;

public record ProductImageRecoveryResponse(
        Long productId,
        ProductStatus status,
        String mainImageKey,
        List<String> thumbnailImageKeys,
        List<String> detailImageKeys
) {
}
