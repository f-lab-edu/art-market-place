package com.woobeee.artmarketplace.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "product_tags",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "tag_id"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductTag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long tagId;

    @Builder
    private ProductTag(Long productId, Long tagId) {
        this.productId = productId;
        this.tagId = tagId;
    }

    public static ProductTag create(Long productId, Long tagId) {
        return ProductTag.builder()
                .productId(productId)
                .tagId(tagId)
                .build();
    }
}
