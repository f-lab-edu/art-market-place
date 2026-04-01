package com.woobeee.auth.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.woobeee.auth.dto.provider.MessageEvent;
import com.woobeee.auth.entity.Outbox;
import com.woobeee.auth.entity.enums.EventStatus;
import com.woobeee.auth.entity.enums.EventType;
import com.woobeee.auth.repository.OutBoxCustomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class MessageEventListener {
    @Value("${spring.config.activate.on-profile}")
    private String profile;

    private final OutBoxCustomRepository outBoxMessageCustomRepository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleEvent(MessageEvent event) {
        log.info("send with redis: {}", event.message());

        String topic = profile + event.topic();
        String key = event.key();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            String payload = objectMapper.writeValueAsString(event.message());

            Outbox outboxMessage = Outbox.builder()
                .id(event.eventId())
                .type(EventType.TRIGGER)
                .status(EventStatus.NEW)
                .topic(topic)
                .key(key)
                .payload(payload)
                .attempts(0)
                .lastError(null)
                .createdAt(LocalDateTime.now())
                .nextAttemptAt(LocalDateTime.now())
                .sentAt(null).build();

            outBoxMessageCustomRepository.insertNew(
                    event.eventId(), EventType.TRIGGER, EventStatus.NEW, topic, key, payload, LocalDateTime.now());

            log.info("Outbox stored. eventId={}, topic={}, key={}",
                    outboxMessage.getId(), topic, key);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
