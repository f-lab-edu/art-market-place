package com.woobeee.artmarketplace.product.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ProductImageRecoveryRequest(
        @NotBlank(message = "Main image key is required")
        @Size(max = 1000, message = "Main image key must be 1000 characters or fewer")
        String mainImageKey,

        @Size(max = 20, message = "Thumbnail images must contain 20 items or fewer")
        List<
                @NotBlank(message = "Thumbnail image key must not be blank")
                @Size(max = 1000, message = "Thumbnail image key must be 1000 characters or fewer")
                String
        > thumbnailImageKeys,

        @Size(max = 20, message = "Detail images must contain 20 items or fewer")
        List<
                @NotBlank(message = "Detail image key must not be blank")
                @Size(max = 1000, message = "Detail image key must be 1000 characters or fewer")
                String
        > detailImageKeys
) {
}
