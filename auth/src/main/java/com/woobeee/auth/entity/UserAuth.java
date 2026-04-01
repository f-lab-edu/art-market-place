package com.woobeee.auth.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class UserAuth {
    @EmbeddedId
    private UserAuthId id;

    @CreationTimestamp
    private LocalDateTime createdAt = LocalDateTime.now();

    @Embeddable
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @Builder
    public static class UserAuthId implements Serializable {
        private UUID userId;
        private Long authId;
    }

    public Long getAuthId(){
        return getId().authId;
    }
}
