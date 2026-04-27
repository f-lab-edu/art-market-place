package com.woobeee.artmarketplace.blog.repository;

import com.woobeee.artmarketplace.blog.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    interface CategoryCount {
        Long getCategoryId();
        long getCnt();
    }

    @Query(
            value =
            """
                SELECT p.category_id AS categoryId, COUNT(*) AS cnt
                FROM post p
                WHERE p.category_id IN (:categoryIds)
                GROUP BY p.category_id
            """,
            nativeQuery = true
    )
    List<CategoryCount> countGroupByCategoryId(@Param("categoryIds") Collection<Long> categoryIds);

    void deleteAllByCategoryIdIn(List<Long> ids);

    Page<Post> findAllByCategoryIdIn(List<Long> ids, Pageable pageable);

    Page<Post> findByTitleEnContainingIgnoreCaseOrTextEnContainingIgnoreCaseOrderByCreatedAtDesc(String titleEn, String textEn, Pageable pageable);

    Page<Post> findByTitleKoContainingIgnoreCaseOrTextKoContainingIgnoreCaseOrderByCreatedAtDesc(String titleKo, String textKo, Pageable pageable);

    Page<Post> findByCategoryIdInAndTitleEnContainingIgnoreCaseOrCategoryIdInAndTextEnContainingIgnoreCaseOrderByCreatedAtDesc(
            List<Long> categoryId1, String titleEn,
            List<Long> categoryId2, String textEn,
            Pageable pageable
    );

    Page<Post> findByCategoryIdInAndTitleKoContainingIgnoreCaseOrCategoryIdInAndTextKoContainingIgnoreCaseOrderByCreatedAtDesc(
            List<Long> categoryId1, String titleKo,
            List<Long> categoryId2, String textKo,
            Pageable pageable
    );
}
