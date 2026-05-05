package com.woobeee.artmarketplace.product.api.response;

import java.util.List;

public record PresignedUploadBatchResponse(
        PresignedUploadResponse mainImage,
        List<PresignedUploadResponse> thumbnailImages,
        List<PresignedUploadResponse> detailImages
) {
}
