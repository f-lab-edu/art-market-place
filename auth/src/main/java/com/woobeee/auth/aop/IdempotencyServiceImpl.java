package com.woobeee.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.woobeee.auth.dto.IdempotencyResult;
import com.woobeee.auth.entity.IdempotencyRecord;
import com.woobeee.auth.exception.CustomConflictException;
import com.woobeee.auth.exception.ErrorCode;
import com.woobeee.auth.repository.IdempotencyRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;


@RequiredArgsConstructor
@Slf4j
@Service
@Transactional
public class IdempotencyServiceImpl implements IdempotencyService{
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdempotencyResult begin(String clientId, String domainKey, String requestHash) {
        try {
            idempotencyRecordRepository.saveAndFlush(
                    (IdempotencyRecord.inProgress(clientId, domainKey, requestHash, Duration.ofHours(24))));
            return new IdempotencyResult(false, false, null, null);
        } catch (DataIntegrityViolationException dup) {

            return idempotencyRecordRepository
                    .findByClientIdAndDomainKey(clientId, domainKey)
                    .map(existing -> {
                        if (!existing.getRequestHash().equals(requestHash)) {
                            throw new CustomConflictException(
                                    ErrorCode.api_idempotencyKeyConflictStopTryingToMessWithMyServer
                            );
                        }
                        if (existing.getStatus() == IdempotencyRecord.Status.COMPLETED
                        || existing.getStatus() == IdempotencyRecord.Status.FAILED) {
                            return new IdempotencyResult(
                                    true,
                                    false,
                                    existing.getResponseCode(),
                                    existing.getResponseBody()
                            );
                        }
                        return new IdempotencyResult(false, true, null, null);
                    })
                    .orElseThrow(() ->
                            // DB Unique 키가 저장은 막겠지만 MySql 같은 경우는 Commit 전에 조회가 될 수 있기 때문에 안전상 에러 호출
                            new CustomConflictException(
                                    ErrorCode.api_idempotencyKeyConflict)
                    );
        }
    }

    @Override
    public void complete(String clientId, String domainKey, int code, Object body) {
        IdempotencyRecord r = idempotencyRecordRepository
                .findByClientIdAndDomainKey(clientId, domainKey).orElseThrow();

        try {
            String b = objectMapper.writeValueAsString(body);
            r.markCompleted(code, b);
            idempotencyRecordRepository.saveAndFlush(r);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize response body", e);
        }
    }

    @Override
    public void fail(String clientId, String domainKey, int code, Object body) {
        IdempotencyRecord r = idempotencyRecordRepository
                .findByClientIdAndDomainKey(clientId, domainKey).orElseThrow();

        try {
            String b = objectMapper.writeValueAsString(body);
            r.markFailed(code, b);
            idempotencyRecordRepository.saveAndFlush(r);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize response body", e);
        }
    }
}
