package com.woobeee.artmarketplace.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "product_images")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String fileKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductImageType type;

    @Column(nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private Long productId;


    @Builder
    private ProductImage(
            Long productId,
            String fileKey,
            ProductImageType type,
            int sortOrder
    ) {
        this.productId = productId;
        this.fileKey = fileKey;
        this.type = type;
        this.sortOrder = sortOrder;
    }

    public static ProductImage create(
            Long productId,
            String fileKey,
            ProductImageType type,
            int sortOrder
    ) {
        return ProductImage.builder()
                .productId(productId)
                .fileKey(fileKey)
                .type(type)
                .sortOrder(sortOrder)
                .build();
    }
}
