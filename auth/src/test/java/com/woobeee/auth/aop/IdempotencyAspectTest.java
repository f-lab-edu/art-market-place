package com.woobeee.auth.aop;

import com.woobeee.auth.dto.IdempotencyResult;
import com.woobeee.auth.dto.response.ApiResponse;
import com.woobeee.auth.exception.ErrorCode;
import com.woobeee.auth.exception.UserNotFoundException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdempotencyAspectTest {
    private final FakeIdempotencyService idempotencyService = new FakeIdempotencyService();
    private final IdempotencyAspect aspect = new IdempotencyAspect(idempotencyService, new ObjectMapper());

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void wrap_whenAnnotationDisabled_skipsIdempotencyChecks() throws Throwable {
        Object result = aspect.wrap(proceedingJoinPoint(() -> "ok"), annotation("disabled"));

        assertThat(result).isEqualTo("ok");
        assertThat(idempotencyService.beginCalled).isFalse();
    }

    @Test
    void wrap_withoutRequestContext_throwsIllegalStateException() {
        assertThatThrownBy(() -> aspect.wrap(proceedingJoinPoint(() -> "ok"), annotation("enabled")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No request context");
    }

    @Test
    void wrap_withoutClientRequestUuid_returnsBadRequest() throws Throwable {
        setRequest(request(null, "domain-1", null));

        Object result = aspect.wrap(proceedingJoinPoint(() -> "ok"), annotation("enabled"));

        ResponseEntity<?> response = (ResponseEntity<?>) result;
        ApiResponse<?> body = (ApiResponse<?>) response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.header().message()).isEqualTo("Client-Request-Uuid required");
        assertThat(idempotencyService.beginCalled).isFalse();
    }

    @Test
    void wrap_withoutDomainId_returnsBadRequest() throws Throwable {
        setRequest(request("client-1", null, null));

        Object result = aspect.wrap(proceedingJoinPoint(() -> "ok"), annotation("enabled"));

        ResponseEntity<?> response = (ResponseEntity<?>) result;
        ApiResponse<?> body = (ApiResponse<?>) response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.header().message()).isEqualTo("Domain-Id required");
        assertThat(idempotencyService.beginCalled).isFalse();
    }

    @Test
    void wrap_whenRequestInProgress_returnsConflict() throws Throwable {
        setRequest(request("client-1", "domain-1", "{\"name\":\"woobeee\"}"));
        idempotencyService.beginResult = new IdempotencyResult(false, true, null, null);

        Object result = aspect.wrap(proceedingJoinPoint(() -> "ok"), annotation("enabled"));

        ResponseEntity<?> response = (ResponseEntity<?>) result;
        ApiResponse<?> body = (ApiResponse<?>) response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(body.header().message()).isEqualTo("Request is in progress");
    }

    @Test
    void wrap_whenCachedResponseExists_returnsCachedResponseEntity() throws Throwable {
        setRequest(request("client-1", "domain-1", null));
        idempotencyService.beginResult = new IdempotencyResult(true, false, 201, "{\"result\":\"cached\"}");

        Object result = aspect.wrap(proceedingJoinPoint(() -> "ignored"), annotation("enabled"));

        ResponseEntity<?> response = (ResponseEntity<?>) result;
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(Map.of("result", "cached"));
    }

    @Test
    void wrap_whenProceedReturnsResponseEntity_completesWithReturnedStatusAndBody() throws Throwable {
        setRequest(request("client-1", "domain-1", "{\"hello\":\"world\"}"));
        idempotencyService.beginResult = new IdempotencyResult(false, false, null, null);
        ResponseEntity<Map<String, String>> original = ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("token", "issued"));

        Object result = aspect.wrap(proceedingJoinPoint(() -> original), annotation("enabled"));

        assertThat(result).isEqualTo(original);
        assertThat(idempotencyService.completed).isTrue();
        assertThat(idempotencyService.completeCode).isEqualTo(201);
        assertThat(idempotencyService.completeBody).isEqualTo(Map.of("token", "issued"));
    }

    @Test
    void wrap_whenProceedReturnsPlainObject_completesWithOkStatus() throws Throwable {
        setRequest(request("client-1", "domain-1", null));
        idempotencyService.beginResult = new IdempotencyResult(false, false, null, null);

        Object result = aspect.wrap(proceedingJoinPoint(() -> Map.of("token", "issued")), annotation("enabled"));

        assertThat(result).isEqualTo(Map.of("token", "issued"));
        assertThat(idempotencyService.completed).isTrue();
        assertThat(idempotencyService.completeCode).isEqualTo(200);
        assertThat(idempotencyService.completeBody).isEqualTo(Map.of("token", "issued"));
    }

    @Test
    void wrap_whenProceedThrows_recordsFailureAndRethrows() throws Throwable {
        setRequest(request("client-1", "domain-1", null));
        idempotencyService.beginResult = new IdempotencyResult(false, false, null, null);

        assertThatThrownBy(() -> aspect.wrap(
                proceedingJoinPoint(() -> {
                    throw new UserNotFoundException(ErrorCode.login_userNotFound);
                }),
                annotation("enabled")
        )).isInstanceOf(UserNotFoundException.class)
                .hasMessage("login_userNotFound");

        assertThat(idempotencyService.failed).isTrue();
        assertThat(idempotencyService.failCode).isEqualTo(500);
        ApiResponse<?> failBody = (ApiResponse<?>) idempotencyService.failBody;
        assertThat(failBody.header().resultCode()).isEqualTo(404);
        assertThat(failBody.header().message()).isEqualTo("login_userNotFound");
    }

    @Test
    void wrap_whenProceedThrowsResponseStatusException_usesResolvedStatusInBody() throws Throwable {
        setRequest(request("client-1", "domain-1", null));
        idempotencyService.beginResult = new IdempotencyResult(false, false, null, null);

        assertThatThrownBy(() -> aspect.wrap(
                proceedingJoinPoint(() -> {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "downstream");
                }),
                annotation("enabled")
        )).isInstanceOf(ResponseStatusException.class);

        assertThat(idempotencyService.failed).isTrue();
        ApiResponse<?> failBody = (ApiResponse<?>) idempotencyService.failBody;
        assertThat(failBody.header().resultCode()).isEqualTo(502);
        assertThat(failBody.header().message()).isEqualTo("502 BAD_GATEWAY \"downstream\"");
    }

    private static Idempotent annotation(String methodName) throws NoSuchMethodException {
        Method method = AnnotatedMethods.class.getDeclaredMethod(methodName);
        return method.getAnnotation(Idempotent.class);
    }

    private static ProceedingJoinPoint proceedingJoinPoint(ThrowingSupplier supplier) {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "proceed" -> supplier.get();
            case "toString" -> "ProceedingJoinPointProxy";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> null;
        };
        return (ProceedingJoinPoint) Proxy.newProxyInstance(
                ProceedingJoinPoint.class.getClassLoader(),
                new Class<?>[]{ProceedingJoinPoint.class},
                handler
        );
    }

    private static void setRequest(MockHttpServletRequest request) {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private static MockHttpServletRequest request(String clientId, String domainId, String body) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/auth/login");
        if (clientId != null) {
            request.addHeader("Client-Request-Uuid", clientId);
        }
        if (domainId != null) {
            request.addHeader("Domain-Id", domainId);
        }
        if (body != null) {
            request.setContent(body.getBytes(StandardCharsets.UTF_8));
        }
        return request;
    }

    @FunctionalInterface
    private interface ThrowingSupplier {
        Object get() throws Throwable;
    }

    private static final class FakeIdempotencyService implements IdempotencyService {
        private IdempotencyResult beginResult;
        private boolean beginCalled;
        private boolean completed;
        private boolean failed;
        private int completeCode;
        private Object completeBody;
        private int failCode;
        private Object failBody;

        @Override
        public IdempotencyResult begin(String clientId, String idemKey, String requestHash) {
            this.beginCalled = true;
            return beginResult;
        }

        @Override
        public void complete(String clientId, String idemKey, int code, Object body) {
            this.completed = true;
            this.completeCode = code;
            this.completeBody = body;
        }

        @Override
        public void fail(String clientId, String idemKey, int code, Object body) {
            this.failed = true;
            this.failCode = code;
            this.failBody = body;
        }
    }

    private static final class AnnotatedMethods {
        @Idempotent
        void enabled() {
        }

        @Idempotent(enabled = false)
        void disabled() {
        }
    }
}
