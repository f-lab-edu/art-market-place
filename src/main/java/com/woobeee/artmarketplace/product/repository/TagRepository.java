package com.woobeee.artmarketplace.product.repository;

import com.woobeee.artmarketplace.product.entity.Tag;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {
    List<Tag> findByNameIn(Collection<String> names);
}
