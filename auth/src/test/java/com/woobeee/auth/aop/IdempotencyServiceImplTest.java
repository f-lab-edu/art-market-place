package com.woobeee.auth.aop;

import com.woobeee.auth.dto.IdempotencyResult;
import com.woobeee.auth.entity.IdempotencyRecord;
import com.woobeee.auth.exception.CustomConflictException;
import com.woobeee.auth.repository.IdempotencyRecordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdempotencyServiceImplTest {
    @Test
    void begin_whenFirstRequest_returnsFreshProceedResult() {
        RepositoryHandler handler = new RepositoryHandler();
        IdempotencyServiceImpl service = service(handler);

        IdempotencyResult result = service.begin("client-1", "domain-1", "hash-1");

        assertThat(result.proceed()).isFalse();
        assertThat(result.inProgress()).isFalse();
        assertThat(result.responseCode()).isNull();
        assertThat(result.responseBody()).isNull();
        assertThat(handler.lastSaved.getClientId()).isEqualTo("client-1");
        assertThat(handler.lastSaved.getDomainKey()).isEqualTo("domain-1");
        assertThat(handler.lastSaved.getRequestHash()).isEqualTo("hash-1");
        assertThat(handler.lastSaved.getStatus()).isEqualTo(IdempotencyRecord.Status.PROGRESS);
        assertThat(handler.lastSaved.getCreatedAt()).isNotNull();
        assertThat(handler.lastSaved.getExpiresAt()).isAfter(handler.lastSaved.getCreatedAt());
    }

    @Test
    void begin_whenDuplicateWithDifferentHash_throwsConflict() {
        RepositoryHandler handler = new RepositoryHandler();
        handler.saveException = new DataIntegrityViolationException("duplicate");
        handler.foundRecord = IdempotencyRecord.inProgress("client-1", "domain-1", "other-hash", Duration.ofHours(1));
        IdempotencyServiceImpl service = service(handler);

        assertThatThrownBy(() -> service.begin("client-1", "domain-1", "hash-1"))
                .isInstanceOf(CustomConflictException.class)
                .hasMessage("api_idempotencyKeyConflictStopTryingToMessWithMyServer");
    }

    @Test
    void begin_whenDuplicateCompleted_returnsCachedResponse() {
        RepositoryHandler handler = new RepositoryHandler();
        handler.saveException = new DataIntegrityViolationException("duplicate");
        handler.foundRecord = IdempotencyRecord.inProgress("client-1", "domain-1", "hash-1", Duration.ofHours(1));
        handler.foundRecord.markCompleted(201, "{\"ok\":true}");
        IdempotencyServiceImpl service = service(handler);

        IdempotencyResult result = service.begin("client-1", "domain-1", "hash-1");

        assertThat(result.proceed()).isTrue();
        assertThat(result.inProgress()).isFalse();
        assertThat(result.responseCode()).isEqualTo(201);
        assertThat(result.responseBody()).isEqualTo("{\"ok\":true}");
    }

    @Test
    void begin_whenDuplicateStillRunning_returnsInProgress() {
        RepositoryHandler handler = new RepositoryHandler();
        handler.saveException = new DataIntegrityViolationException("duplicate");
        handler.foundRecord = IdempotencyRecord.inProgress("client-1", "domain-1", "hash-1", Duration.ofHours(1));
        IdempotencyServiceImpl service = service(handler);

        IdempotencyResult result = service.begin("client-1", "domain-1", "hash-1");

        assertThat(result.proceed()).isFalse();
        assertThat(result.inProgress()).isTrue();
        assertThat(result.responseCode()).isNull();
        assertThat(result.responseBody()).isNull();
    }

    @Test
    void begin_whenDuplicateButLookupMissing_throwsFallbackConflict() {
        RepositoryHandler handler = new RepositoryHandler();
        handler.saveException = new DataIntegrityViolationException("duplicate");
        IdempotencyServiceImpl service = service(handler);

        assertThatThrownBy(() -> service.begin("client-1", "domain-1", "hash-1"))
                .isInstanceOf(CustomConflictException.class)
                .hasMessage("api_idempotencyKeyConflict");
    }

    @Test
    void complete_marksRecordCompletedAndPersistsSerializedBody() {
        RepositoryHandler handler = new RepositoryHandler();
        handler.foundRecord = IdempotencyRecord.inProgress("client-1", "domain-1", "hash-1", Duration.ofHours(1));
        IdempotencyServiceImpl service = service(handler);

        service.complete("client-1", "domain-1", 200, Map.of("result", "ok"));

        assertThat(handler.foundRecord.getStatus()).isEqualTo(IdempotencyRecord.Status.COMPLETED);
        assertThat(handler.foundRecord.getResponseCode()).isEqualTo(200);
        assertThat(handler.foundRecord.getResponseBody()).contains("\"result\":\"ok\"");
        assertThat(handler.lastSaved).isSameAs(handler.foundRecord);
    }

    @Test
    void fail_marksRecordFailedAndPersistsSerializedBody() {
        RepositoryHandler handler = new RepositoryHandler();
        handler.foundRecord = IdempotencyRecord.inProgress("client-1", "domain-1", "hash-1", Duration.ofHours(1));
        IdempotencyServiceImpl service = service(handler);

        service.fail("client-1", "domain-1", 500, Map.of("message", "boom"));

        assertThat(handler.foundRecord.getStatus()).isEqualTo(IdempotencyRecord.Status.FAILED);
        assertThat(handler.foundRecord.getResponseCode()).isEqualTo(500);
        assertThat(handler.foundRecord.getResponseBody()).contains("\"message\":\"boom\"");
        assertThat(handler.lastSaved).isSameAs(handler.foundRecord);
    }

    private static IdempotencyServiceImpl service(RepositoryHandler handler) {
        return new IdempotencyServiceImpl(handler.repository(), new ObjectMapper());
    }

    private static final class RepositoryHandler implements InvocationHandler {
        private RuntimeException saveException;
        private IdempotencyRecord foundRecord;
        private IdempotencyRecord lastSaved;

        private IdempotencyRecordRepository repository() {
            return (IdempotencyRecordRepository) Proxy.newProxyInstance(
                    IdempotencyRecordRepository.class.getClassLoader(),
                    new Class<?>[]{IdempotencyRecordRepository.class},
                    this
            );
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "saveAndFlush" -> saveAndFlush((IdempotencyRecord) args[0]);
                case "findByClientIdAndDomainKey" -> Optional.ofNullable(foundRecord);
                case "toString" -> "RepositoryHandlerProxy";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }

        private IdempotencyRecord saveAndFlush(IdempotencyRecord record) {
            if (saveException != null) {
                throw saveException;
            }
            lastSaved = record;
            return record;
        }
    }
}
