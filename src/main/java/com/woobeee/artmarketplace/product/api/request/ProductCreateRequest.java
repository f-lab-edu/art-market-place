package com.woobeee.artmarketplace.product.api.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record ProductCreateRequest(
        @NotNull(message = "Seller ID is required")
        Long sellerId,

        @NotBlank(message = "Height is required")
        @Size(max = 100, message = "Height must be 100 characters or fewer")
        String height,

        @NotBlank(message = "Width is required")
        @Size(max = 100, message = "Width must be 100 characters or fewer")
        String width,

        @NotBlank(message = "Shape is required")
        @Size(max = 100, message = "Shape must be 100 characters or fewer")
        String shape,

        @NotBlank(message = "Material is required")
        @Size(max = 200, message = "Material must be 200 characters or fewer")
        String material,

        @Size(max = 20, message = "Tags must contain 20 items or fewer")
        List<
                @NotBlank(message = "Tag must not be blank")
                @Size(max = 50, message = "Tag must be 50 characters or fewer")
                String
        > tags,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
        BigDecimal price,

        @NotBlank(message = "Main image key is required")
        @Size(max = 1000, message = "Main image key must be 1000 characters or fewer")
        String mainImageKey,

        @Size(max = 20, message = "Detail images must contain 20 items or fewer")
        List<
                @NotBlank(message = "Detail image key must not be blank")
                @Size(max = 1000, message = "Detail image key must be 1000 characters or fewer")
                String
        > detailImageKeys
) {
}
