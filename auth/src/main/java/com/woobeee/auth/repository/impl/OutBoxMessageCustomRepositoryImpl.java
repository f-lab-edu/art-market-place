package com.woobeee.auth.repository.impl;

import com.woobeee.auth.entity.enums.EventStatus;
import com.woobeee.auth.entity.enums.EventType;
import com.woobeee.auth.repository.OutBoxCustomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Repository
public class OutBoxMessageCustomRepositoryImpl implements OutBoxCustomRepository {
    private final JdbcTemplate jdbcTemplate;

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
        String sql = """
        INSERT INTO outbox (
            id,
            type,
            status,
            topic,
            key,
            payload,
            attempts,
            last_error,
            created_at,
            locked_at,
            next_attempt_at,
            sent_at
        ) VALUES (
            ?, ?, ?, ?, ?, ?::json, 0, NULL, ?, NULL, ?, NULL
        )
        """;

        return jdbcTemplate.update(
                sql,
                id,
                eventType.name(),
                eventStatus.name(),
                topic,
                key,
                payload,
                Timestamp.valueOf(now),
                Timestamp.valueOf(now)
        );
    }

    /**
     * SENDING 상태에서 일정 시간 이상 멈춘 메시지를 FAILED로 복구
     */
    public int recoverStuckSending(LocalDateTime now, Duration threshold) {
        String sql = """
                    UPDATE outbox
                    SET status = 'FAIL',
                        next_attempt_at = ?,
                        locked_at = NULL,
                        last_error = CONCAT(
                            COALESCE(last_error, ''),
                            ' | recovered from stuck SENDING at ',
                            CAST(? AS text)         -- ✅ 여기
                        )
                    WHERE status = 'SENDING'
                      AND locked_at IS NOT NULL
                      AND locked_at < ?
                    """;

        LocalDateTime stuckBefore = now.minus(threshold);

        return jdbcTemplate.update(
                sql,
                Timestamp.valueOf(now),           // next_attempt_at
                Timestamp.valueOf(now),           // error message timestamp
                Timestamp.valueOf(stuckBefore)    // locked_at threshold
        );
    }

    @Override
    public List<OutboxRow> claimBatchForSend(LocalDateTime now, int limit) {
        String sql = """
            WITH cte AS (
              SELECT id
              FROM outbox
              WHERE status IN ('NEW','FAIL')
                AND next_attempt_at <= ?
              ORDER BY created_at
              LIMIT ?
              FOR UPDATE SKIP LOCKED
            )
            UPDATE outbox o
            SET status = 'SENDING',
                attempts = o.attempts + 1,
                locked_at = now(),
                last_error = NULL
            FROM cte
            WHERE o.id = cte.id
            RETURNING
                o.id,
                o.type,
                o.status,
                o.topic,
                o.key,
                o.payload,
                o.attempts,
                o.last_error,
                o.created_at,
                o.locked_at,
                o.next_attempt_at,
                o.sent_at
            """;

        return jdbcTemplate.query(sql, ps -> {
            ps.setTimestamp(1, Timestamp.valueOf(now));
            ps.setInt(2, limit);
        }, (rs, rowNum) -> new OutboxRow(
                (UUID) rs.getObject("id"),
                EventType.valueOf(rs.getString("type")),
                EventStatus.valueOf(rs.getString("status")),
                rs.getString("topic"),
                rs.getString("key"),
                rs.getString("payload"),
                rs.getInt("attempts"),
                rs.getString("last_error"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("locked_at").toLocalDateTime(),
                rs.getTimestamp("next_attempt_at").toLocalDateTime(),
                rs.getTimestamp("sent_at") == null ? null : rs.getTimestamp("sent_at").toLocalDateTime()
        ));
    }

    @Override
    public long markSent(UUID id, LocalDateTime sentAt) {
        return jdbcTemplate.update("""
            UPDATE outbox
            SET status='SENT', sent_at=?, last_error=NULL
            WHERE id=?
            """, Timestamp.valueOf(sentAt), id);
    }

    @Override
    public long markFailed(UUID id, String lastError, LocalDateTime nextAttemptAt) {
        return jdbcTemplate.update("""
            UPDATE outbox
            SET status='FAIL', last_error=?, next_attempt_at=?
            WHERE id=?
            """, lastError, Timestamp.valueOf(nextAttemptAt), id);
    }

    public record OutboxRow(
            UUID id,
            EventType type,
            EventStatus status,
            String topic,
            String key,
            String payload,
            int attempts,
            String lastError,
            LocalDateTime createdAt,
            LocalDateTime lockedAt,
            LocalDateTime nextAttemptAt,
            LocalDateTime sentAt
    ) {}
}
