package com.woobeee.artmarketplace.blog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titleKo;
    private String titleEn;

    @Column(columnDefinition = "text")
    @Setter
    private String textKo;
    @Column(columnDefinition = "text")
    @Setter
    private String textEn;

    @Builder.Default
    private Long views = 0L;

    @CreationTimestamp
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @UpdateTimestamp
    private LocalDateTime updatedAt;

//    @ManyToOne(fetch = FetchType.LAZY)
//    private Category category;
    private Long categoryId;
    private Long memberId;
    private String memberRole;

    public Post(String titleKo, String titleEn, String textKo, String textEn, Long categoryId, Long memberId, String memberRole) {
        this.titleKo = titleKo;
        this.titleEn = titleEn;
        this.textKo = textKo;
        this.textEn = textEn;
        this.categoryId = categoryId;
        this.memberId = memberId;
        this.memberRole = memberRole;
        this.views = 0L;
    }
}
