package com.woobeee.artmarketplace.blog.config;


import com.woobeee.artmarketplace.blog.entity.Category;
import com.woobeee.artmarketplace.blog.entity.Comment;
import com.woobeee.artmarketplace.blog.entity.Post;
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
            Category category = new Category("BACKEND", "BACKEND", null);
            category = categoryRepository.save(category);

            Category category1 = new Category("Spring Batch", "Spring Batch", category.getId());
            category1 = categoryRepository.save(category1);

            Category category2 = new Category("FRONTEND", "FRONTEND", null);
            category2 = categoryRepository.save(category2);

            Category category3 = new Category("NextJS", "NextJS", category2.getId());
            category3 = categoryRepository.save(category3);
        }

        if (false) {
            Post post = new Post("test", "test", "test", "test", 2L, 1L, "ROLE_BUYER");
            //2
            //3602cc93-4121-471b-adeb-a98cfc625af6

            post = postRepository.save(post);

            Comment comment = new Comment("test-comment", post.getId(), null, post.getMemberId(), post.getMemberRole());

            comment = commentRepository.save(comment);
        }

        if (false) {
            Post post = new Post("test nextjs post", "test nextjs post", "test nextjs post", "test nextjs post", 4L, 1L, "ROLE_BUYER");
            //2
            //3602cc93-4121-471b-adeb-a98cfc625af6

            post = postRepository.save(post);
        }
    }
}
