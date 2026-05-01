package com.woobeee.artmarketplace.blog.repository;

import com.woobeee.artmarketplace.blog.entity.Categories;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Categories, Long> {
    List<Categories> findAllByParentId(Long parentId);
}
