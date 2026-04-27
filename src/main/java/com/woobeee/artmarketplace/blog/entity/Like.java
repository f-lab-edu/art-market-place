package com.woobeee.artmarketplace.blog.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class Like {

    @EmbeddedId
    private LikeId id;

    @CreationTimestamp
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Embeddable
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @Builder
    public static class LikeId implements Serializable {
        private Long memberId;
        private String memberRole;
        private Long postId;
    }

    public Like(Long memberId, String memberRole, Long postId) {
        this.id = new LikeId(memberId, memberRole, postId);
    }
}
