package com.woobeee.auth.producer;


import com.woobeee.auth.repository.OutBoxCustomRepository;
import com.woobeee.auth.repository.impl.OutBoxMessageCustomRepositoryImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxProducerScheduler {
    private static final int BATCH_SIZE = 100;

    private final OutBoxCustomRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "1000") // 1초마다
    public void publish() {
        // FAIL, NEW를 조회와 동시에 SENDING으로 상태변경을 원자성으로 묶기
        // FOR UPDATE SKIP LOCKED을 통해서 락 걸린 자원들은 스킵해서 중복되는 자원은 없게 지원
        List<OutBoxMessageCustomRepositoryImpl.OutboxRow> batch =
                outboxRepository.claimBatchForSend(LocalDateTime.now(), BATCH_SIZE);

        if (batch.isEmpty()) return;

        for (var row : batch) {
            try {
                kafkaTemplate.send(row.topic(), row.key(), row.payload()).get();
                outboxRepository.markSent(row.id(), LocalDateTime.now());

                log.info("Outbox sent. id={}, key={}, topic={}", row.id(), row.key(), row.topic());
            } catch (Exception ex) {
                int attempts = row.attempts();
                long delaySeconds = Math.min(300, (long) Math.pow(2, Math.min(attempts, 6)) * 5);
                LocalDateTime nextAttemptAt = LocalDateTime.now().plusSeconds(delaySeconds);

                outboxRepository.markFailed(row.id(), ex.getMessage(), nextAttemptAt);

                log.error("Outbox send failed. id={}, attempts={}, nextAttemptAt={}, err={}",
                        row.id(), attempts, nextAttemptAt, ex.getMessage(), ex);
            }
        }
    }
}
