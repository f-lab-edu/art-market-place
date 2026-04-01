package com.woobeee.auth.producer;

import com.woobeee.auth.entity.enums.EventStatus;
import com.woobeee.auth.entity.enums.EventType;
import com.woobeee.auth.repository.OutBoxCustomRepository;
import com.woobeee.auth.repository.impl.OutBoxMessageCustomRepositoryImpl;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxProducerSchedulerTest {
    @Test
    void publish_whenBatchIsEmpty_doesNothing() {
        RepositoryStub repository = new RepositoryStub();
        StubKafkaTemplate kafkaTemplate = StubKafkaTemplate.success();
        OutboxProducerScheduler scheduler = new OutboxProducerScheduler(repository, kafkaTemplate);

        scheduler.publish();

        assertThat(kafkaTemplate.sentRecords).isEmpty();
        assertThat(repository.markSentIds).isEmpty();
        assertThat(repository.markFailedIds).isEmpty();
    }

    @Test
    void publish_whenKafkaSendSucceeds_marksRowAsSent() {
        RepositoryStub repository = new RepositoryStub();
        StubKafkaTemplate kafkaTemplate = StubKafkaTemplate.success();
        UUID outboxId = UUID.randomUUID();
        repository.batch = List.of(row(outboxId, 1));
        OutboxProducerScheduler scheduler = new OutboxProducerScheduler(repository, kafkaTemplate);

        LocalDateTime before = LocalDateTime.now();
        scheduler.publish();
        LocalDateTime after = LocalDateTime.now();

        assertThat(kafkaTemplate.sentRecords).containsExactly(new SentRecord("topic-a", "user-1", "{\"ok\":true}"));
        assertThat(repository.markSentIds).containsExactly(outboxId);
        assertThat(repository.markFailedIds).isEmpty();
        assertThat(repository.markSentAtValues).hasSize(1);
        assertThat(repository.markSentAtValues.getFirst()).isBetween(before, after);
    }

    @Test
    void publish_whenKafkaSendFails_marksRowAsFailedWithBackoff() {
        RepositoryStub repository = new RepositoryStub();
        StubKafkaTemplate kafkaTemplate = StubKafkaTemplate.failure("kafka down");
        UUID outboxId = UUID.randomUUID();
        repository.batch = List.of(row(outboxId, 3));
        OutboxProducerScheduler scheduler = new OutboxProducerScheduler(repository, kafkaTemplate);

        LocalDateTime before = LocalDateTime.now();
        scheduler.publish();
        LocalDateTime after = LocalDateTime.now();

        assertThat(kafkaTemplate.sentRecords).containsExactly(new SentRecord("topic-a", "user-1", "{\"ok\":true}"));
        assertThat(repository.markSentIds).isEmpty();
        assertThat(repository.markFailedIds).containsExactly(outboxId);
        assertThat(repository.lastError).isEqualTo("java.lang.IllegalStateException: kafka down");
        assertThat(repository.lastNextAttemptAt)
                .isAfterOrEqualTo(before.plusSeconds(40))
                .isBeforeOrEqualTo(after.plusSeconds(40));
    }

    private static OutBoxMessageCustomRepositoryImpl.OutboxRow row(UUID id, int attempts) {
        LocalDateTime now = LocalDateTime.now();
        return new OutBoxMessageCustomRepositoryImpl.OutboxRow(
                id,
                EventType.TRIGGER,
                EventStatus.NEW,
                "topic-a",
                "user-1",
                "{\"ok\":true}",
                attempts,
                null,
                now,
                now,
                now,
                null
        );
    }

    private record SentRecord(String topic, String key, String payload) {
    }

    private static final class RepositoryStub implements OutBoxCustomRepository {
        private List<OutBoxMessageCustomRepositoryImpl.OutboxRow> batch = List.of();
        private final List<UUID> markSentIds = new ArrayList<>();
        private final List<UUID> markFailedIds = new ArrayList<>();
        private final List<LocalDateTime> markSentAtValues = new ArrayList<>();
        private String lastError;
        private LocalDateTime lastNextAttemptAt;

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
        public int recoverStuckSending(LocalDateTime now, java.time.Duration threshold) {
            throw new UnsupportedOperationException("recoverStuckSending");
        }

        @Override
        public List<OutBoxMessageCustomRepositoryImpl.OutboxRow> claimBatchForSend(LocalDateTime now, int limit) {
            return batch;
        }

        @Override
        public long markSent(UUID id, LocalDateTime sentAt) {
            markSentIds.add(id);
            markSentAtValues.add(sentAt);
            return 1L;
        }

        @Override
        public long markFailed(UUID id, String lastError, LocalDateTime nextAttemptAt) {
            markFailedIds.add(id);
            this.lastError = lastError;
            this.lastNextAttemptAt = nextAttemptAt;
            return 1L;
        }
    }

    private static final class StubKafkaTemplate extends KafkaTemplate<String, String> {
        private final RuntimeException sendFailure;
        private final List<SentRecord> sentRecords = new ArrayList<>();

        private StubKafkaTemplate(RuntimeException sendFailure) {
            super(new DefaultKafkaProducerFactory<>(Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")));
            this.sendFailure = sendFailure;
        }

        private static StubKafkaTemplate success() {
            return new StubKafkaTemplate(null);
        }

        private static StubKafkaTemplate failure(String message) {
            return new StubKafkaTemplate(new IllegalStateException(message));
        }

        @Override
        public CompletableFuture<SendResult<String, String>> send(String topic, String key, String data) {
            sentRecords.add(new SentRecord(topic, key, data));
            if (sendFailure != null) {
                return CompletableFuture.failedFuture(sendFailure);
            }
            return CompletableFuture.completedFuture(null);
        }
    }
}
