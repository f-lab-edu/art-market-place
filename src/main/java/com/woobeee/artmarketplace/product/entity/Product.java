package com.woobeee.artmarketplace.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String height;

    @Column(nullable = false, length = 100)
    private String width;

    @Column(nullable = false, length = 100)
    private String shape;

    @Column(nullable = false, length = 200)
    private String material;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean active;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Long sellerId;

    @Builder
    private Product(
            Long sellerId,
            String height,
            String width,
            String shape,
            String material,
            BigDecimal price,
            boolean active,
            ProductStatus status,
            LocalDateTime createdAt
    ) {
        this.sellerId = sellerId;
        this.height = height;
        this.width = width;
        this.shape = shape;
        this.material = material;
        this.price = price;
        this.active = active;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Product create(
            Long sellerId,
            String height,
            String width,
            String shape,
            String material,
            BigDecimal price
    ) {
        return Product.builder()
                .sellerId(sellerId)
                .height(height)
                .width(width)
                .shape(shape)
                .material(material)
                .price(price)
                .active(false)
                .status(ProductStatus.IMAGE_PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public void activate() {
        this.active = true;
        this.status = ProductStatus.ACTIVE;
    }

    public void markImagePending() {
        this.active = false;
        this.status = ProductStatus.IMAGE_PENDING;
    }

    public void markImageFailed() {
        this.active = false;
        this.status = ProductStatus.IMAGE_FAILED;
    }
}
