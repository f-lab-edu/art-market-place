package com.woobeee.auth.entity;

import jakarta.persistence.*;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(columnNames = {"clientId", "domainKey"})
)
public class IdempotencyRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String clientId;
    private String domainKey;
    private String requestHash;


    @Enumerated(EnumType.STRING)
    private Status status;

    private Integer responseCode;
    @Lob
    private String responseBody;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public enum Status { PROGRESS, COMPLETED, FAILED }


    public static IdempotencyRecord inProgress(
            String clientId, String domainKey, String requestHash, Duration ttl
    ) {
        IdempotencyRecord r = new IdempotencyRecord();
        r.clientId = clientId;
        r.domainKey = domainKey;
        r.requestHash = requestHash;
        r.status = Status.PROGRESS;
        r.createdAt = LocalDateTime.now();
        r.expiresAt = r.createdAt.plus(ttl);
        return r;
    }

    public void markCompleted(int responseCode, String responseBody) {
        this.status = Status.COMPLETED;
        this.responseCode = responseCode;
        this.responseBody = responseBody;
    }


    public void markFailed(int responseCode, String responseBody) {
        this.status = Status.FAILED;
        this.responseCode = responseCode;
        this.responseBody = responseBody;
    }

    public Long getId() {
        return id;
    }

    public String getClientId() {
        return clientId;
    }

    public String getDomainKey() {
        return domainKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public Status getStatus() {
        return status;
    }

    public Integer getResponseCode() {
        return responseCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

}
