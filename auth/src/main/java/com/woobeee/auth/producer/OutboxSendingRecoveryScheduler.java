package com.woobeee.auth.producer;

import com.woobeee.auth.repository.OutBoxCustomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxSendingRecoveryScheduler {
    private static final Duration STUCK_THRESHOLD = Duration.ofMinutes(10);

    private final OutBoxCustomRepository recoveryRepository;

    @Scheduled(fixedDelayString = "6000")
    public void recoverStuckSending() {
        LocalDateTime now = LocalDateTime.now();

        int recovered = recoveryRepository.recoverStuckSending(now, STUCK_THRESHOLD);

        if (recovered > 0) {
            log.warn("Recovered {} stuck SENDING outbox messages", recovered);
        }
    }
}
