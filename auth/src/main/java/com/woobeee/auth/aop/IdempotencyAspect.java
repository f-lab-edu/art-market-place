package com.woobeee.auth.aop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.woobeee.auth.dto.IdempotencyResult;
import com.woobeee.auth.dto.response.ApiResponse;
import com.woobeee.auth.exception.*;
import com.woobeee.auth.service.IdempotencyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.WebUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyAspect {
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(idempotent)")
    public Object wrap(ProceedingJoinPoint pjp, Idempotent idempotent) throws Throwable {
        if (!idempotent.enabled()) {
            return pjp.proceed();
        }

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) throw new IllegalStateException("No request context");
        HttpServletRequest req = attrs.getRequest();

        String clientId = req.getHeader("Client-Request-Uuid");
        if (clientId == null || clientId.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.fail(HttpStatus.BAD_REQUEST, "Client-Request-Uuid required"));
        }

        String domainId = req.getHeader("Domain-Id");
        if (domainId == null || domainId.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.fail(HttpStatus.BAD_REQUEST, "Domain-Id required"));
        }

        String requestHash = buildRequestHash(req);
        IdempotencyResult begin = idempotencyService.begin(clientId, domainId, requestHash);

        if (begin.inProgress()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.fail(HttpStatus.CONFLICT, "Request is in progress"));
        }

        if (begin.proceed()) {
            Object cached = objectMapper.readValue(begin.responseBody(), Object.class);
            return ResponseEntity.status(begin.responseCode() != null ? begin.responseCode() : 200).body(cached);
        }

        try {
            Object result = pjp.proceed();

            if (result instanceof ResponseEntity<?> re) {
                idempotencyService.complete(clientId, domainId, re.getStatusCode().value(), re.getBody());
            } else {
                idempotencyService.complete(clientId, domainId, HttpStatus.OK.value(), result);
            }
            return result;

        } catch (Throwable t) {

            HttpStatus status = resolveStatus(t);
            ApiResponse<?> failBody = ApiResponse.fail(status, safeMessage(t));

            idempotencyService.fail(
                    clientId,
                    domainId,
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    failBody);
            throw t;
        }
    }

    private String safeMessage(Throwable t) {
        String msg = t.getMessage();
        return (msg == null || msg.isBlank()) ? "Unexpected error" : msg;
    }

    private String buildRequestHash(HttpServletRequest req) {
        String method = req.getMethod();
        String uri = req.getRequestURI();
        String query = req.getQueryString() == null ? "" : req.getQueryString();

        byte[] bodyBytes = new byte[0];
        ContentCachingRequestWrapper wrapper =
                WebUtils.getNativeRequest(req, ContentCachingRequestWrapper.class);

        if (wrapper != null) {
            bodyBytes = wrapper.getContentAsByteArray();
        }

        String bodyBase64 = bodyBytes.length == 0
                ? ""
                : Base64.getEncoder().encodeToString(bodyBytes);

        String base = method + "|" + uri + "|" + query + "|" + bodyBase64;

        return DigestUtils.md5DigestAsHex(base.getBytes(StandardCharsets.UTF_8));
    }

    private HttpStatus resolveStatus(Throwable t) {
        // 401
        if (t instanceof JwtExpiredException) return HttpStatus.UNAUTHORIZED;
        if (t instanceof JwtNotValidException) return HttpStatus.UNAUTHORIZED;

        // 400
        if (t instanceof PasswordNotMatchException) return HttpStatus.BAD_REQUEST;
        if (t instanceof UserConflictException) return HttpStatus.CONFLICT;

        // 404
        if (t instanceof UserNotFoundException) return HttpStatus.NOT_FOUND;

        // 409
        if (t instanceof CustomConflictException) return HttpStatus.CONFLICT;

        if (t instanceof ResponseStatusException rse)
            return HttpStatus.valueOf(rse.getStatusCode().value());

        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
