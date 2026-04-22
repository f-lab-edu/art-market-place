package com.woobeee.artmarketplace.product.repository;

import com.woobeee.artmarketplace.product.entity.ProductTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductTagRepository extends JpaRepository<ProductTag, Long> {
}
