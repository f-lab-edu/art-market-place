package com.woobeee.artmarketplace.product.api.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ProductImagePresignedUrlBatchRequest(
        @Valid
        @NotNull(message = "Main image is required")
        ProductImagePresignedUrlRequest mainImage,

        @Valid
        @Size(max = 20, message = "Thumbnail images must contain 20 items or fewer")
        List<ProductImagePresignedUrlRequest> thumbnailImages,

        @Valid
        @Size(max = 20, message = "Detail images must contain 20 items or fewer")
        List<ProductImagePresignedUrlRequest> detailImages
) {
}
