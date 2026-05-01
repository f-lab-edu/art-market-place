package com.woobeee.artmarketplace.blog.repository;

import com.woobeee.artmarketplace.blog.entity.Comments;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comments, Long> {
    List<Comments> findAllByPostId(Long postId);
}
