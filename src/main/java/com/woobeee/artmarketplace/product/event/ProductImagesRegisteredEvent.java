package com.woobeee.artmarketplace.product.event;

import java.util.List;
import java.util.Map;

public record ProductImagesRegisteredEvent(
        Long productId,
        List<String> tempFileKeys,
        Map<String, String> tempToProductFileKeys
) {
}
