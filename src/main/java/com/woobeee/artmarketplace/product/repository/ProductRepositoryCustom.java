package com.woobeee.artmarketplace.product.repository;

import com.woobeee.artmarketplace.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepositoryCustom {
    Page<Product> findActiveProductsByFilters(String tagName, String artist, Pageable pageable);
}
