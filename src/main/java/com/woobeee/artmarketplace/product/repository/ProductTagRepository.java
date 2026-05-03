package com.woobeee.artmarketplace.product.repository;

import com.woobeee.artmarketplace.product.entity.ProductTag;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductTagRepository extends JpaRepository<ProductTag, Long> {
    List<ProductTag> findByProductIdIn(Collection<Long> productIds);
}
