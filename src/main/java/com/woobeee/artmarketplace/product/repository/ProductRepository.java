package com.woobeee.artmarketplace.product.repository;

import com.woobeee.artmarketplace.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
