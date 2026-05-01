package com.woobeee.artmarketplace.blog.config;


import com.woobeee.artmarketplace.blog.entity.Categories;
import com.woobeee.artmarketplace.blog.entity.Comments;
import com.woobeee.artmarketplace.blog.entity.Posts;
import com.woobeee.artmarketplace.blog.repository.CategoryRepository;
import com.woobeee.artmarketplace.blog.repository.CommentRepository;
import com.woobeee.artmarketplace.blog.repository.PostRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InitConfig {
    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    @PostConstruct
    public void init() {
        if (false) {
            Categories category = new Categories("BACKEND", "BACKEND", null);
            category = categoryRepository.save(category);

            Categories category1 = new Categories("Spring Batch", "Spring Batch", category.getId());
            category1 = categoryRepository.save(category1);

            Categories category2 = new Categories("FRONTEND", "FRONTEND", null);
            category2 = categoryRepository.save(category2);

            Categories category3 = new Categories("NextJS", "NextJS", category2.getId());
            category3 = categoryRepository.save(category3);
        }

        if (false) {
            Posts post = new Posts("test", "test", "test", "test", 2L, 1L, "ROLE_BUYER");
            //2
            //3602cc93-4121-471b-adeb-a98cfc625af6

            post = postRepository.save(post);

            Comments comment = new Comments("test-comment", post.getId(), null, post.getMemberId(), post.getMemberRole());

            comment = commentRepository.save(comment);
        }

        if (false) {
            Posts post = new Posts("test nextjs post", "test nextjs post", "test nextjs post", "test nextjs post", 4L, 1L, "ROLE_BUYER");
            //2
            //3602cc93-4121-471b-adeb-a98cfc625af6

            post = postRepository.save(post);
        }
    }
}
