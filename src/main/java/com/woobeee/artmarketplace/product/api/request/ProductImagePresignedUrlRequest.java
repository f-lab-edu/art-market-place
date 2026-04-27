package com.woobeee.artmarketplace.product.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductImagePresignedUrlRequest(
        @NotBlank(message = "File name is required")
        @Size(max = 255, message = "File name must be 255 characters or fewer")
        String fileName,

        @NotBlank(message = "Content type is required")
        @Size(max = 100, message = "Content type must be 100 characters or fewer")
        String contentType
) {
}
