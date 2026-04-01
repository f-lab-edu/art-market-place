package com.woobeee.auth.repository;

import com.woobeee.auth.entity.enums.EventStatus;
import com.woobeee.auth.entity.enums.EventType;
import com.woobeee.auth.repository.impl.OutBoxMessageCustomRepositoryImpl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OutBoxCustomRepository {
    int insertNew(
            UUID id,
            EventType eventType,
            EventStatus eventStatus,
            String topic,
            String key,
            String payload,
            LocalDateTime now
    );
    int recoverStuckSending(LocalDateTime now, Duration threshold);
    List<OutBoxMessageCustomRepositoryImpl.OutboxRow> claimBatchForSend(LocalDateTime now, int limit);
    long markSent(UUID id, LocalDateTime sentAt);
    long markFailed(UUID id, String lastError, LocalDateTime nextAttemptAt);
}
