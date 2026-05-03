package com.woobeee.artmarketplace.product.repository;

import com.woobeee.artmarketplace.product.entity.ProductImage;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    void deleteByProductId(Long productId);

    List<ProductImage> findByProductIdInOrderByProductIdAscSortOrderAsc(Collection<Long> productIds);
}
