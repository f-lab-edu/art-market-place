package com.woobeee.auth.producer;

import com.woobeee.auth.entity.enums.EventStatus;
import com.woobeee.auth.entity.enums.EventType;
import com.woobeee.auth.repository.OutBoxCustomRepository;
import com.woobeee.auth.repository.impl.OutBoxMessageCustomRepositoryImpl;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxSendingRecoverySchedulerTest {
    @Test
    void recoverStuckSending_usesTenMinuteThreshold() {
        RecoveryRepositoryStub repository = new RecoveryRepositoryStub();
        OutboxSendingRecoveryScheduler scheduler = new OutboxSendingRecoveryScheduler(repository);

        LocalDateTime before = LocalDateTime.now();
        scheduler.recoverStuckSending();
        LocalDateTime after = LocalDateTime.now();

        assertThat(repository.called).isTrue();
        assertThat(repository.threshold).isEqualTo(Duration.ofMinutes(10));
        assertThat(repository.now).isBetween(before, after);
    }

    @Test
    void recoverStuckSending_whenRepositoryRecoversRows_stillCompletesNormally() {
        RecoveryRepositoryStub repository = new RecoveryRepositoryStub();
        repository.recoveredCount = 3;
        OutboxSendingRecoveryScheduler scheduler = new OutboxSendingRecoveryScheduler(repository);

        scheduler.recoverStuckSending();

        assertThat(repository.called).isTrue();
        assertThat(repository.recoveredCount).isEqualTo(3);
    }

    private static final class RecoveryRepositoryStub implements OutBoxCustomRepository {
        private boolean called;
        private int recoveredCount;
        private LocalDateTime now;
        private Duration threshold;

        @Override
        public int insertNew(
                UUID id,
                EventType eventType,
                EventStatus eventStatus,
                String topic,
                String key,
                String payload,
                LocalDateTime now
        ) {
            throw new UnsupportedOperationException("insertNew");
        }

        @Override
        public int recoverStuckSending(LocalDateTime now, Duration threshold) {
            called = true;
            this.now = now;
            this.threshold = threshold;
            return recoveredCount;
        }

        @Override
        public List<OutBoxMessageCustomRepositoryImpl.OutboxRow> claimBatchForSend(LocalDateTime now, int limit) {
            throw new UnsupportedOperationException("claimBatchForSend");
        }

        @Override
        public long markSent(UUID id, LocalDateTime sentAt) {
            throw new UnsupportedOperationException("markSent");
        }

        @Override
        public long markFailed(UUID id, String lastError, LocalDateTime nextAttemptAt) {
            throw new UnsupportedOperationException("markFailed");
        }
    }
}
