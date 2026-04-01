package com.woobeee.auth.entity;

import com.woobeee.auth.entity.enums.EventStatus;
import com.woobeee.auth.entity.enums.EventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Outbox {
    @Id
    @UuidGenerator
    private UUID id;

    @Column(name="type", nullable=false)
    private EventType type;

    @Column(name="status", nullable=false)
    private EventStatus status;

    @Column(name="topic", nullable=false)
    private String topic;

    @Column(name="key", nullable=false)
    private String key;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="payload", nullable=false, columnDefinition = "json")
    private String payload;

    @Column(name="attempts", nullable=false)
    private int attempts;

    @Column(name="last_error")
    private String lastError;

    @Column(name="created_at", nullable=false)
    private LocalDateTime createdAt;

    @Column(name="locked_at")
    private LocalDateTime lockedAt;

    @Column(name="next_attempt_at", nullable=false)
    private LocalDateTime nextAttemptAt;

    @Column(name="sent_at")
    private LocalDateTime sentAt;
}
