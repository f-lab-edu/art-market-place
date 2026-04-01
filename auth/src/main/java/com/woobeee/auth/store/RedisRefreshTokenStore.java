package com.woobeee.auth.store;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {
    private static final String LOGIN_ID_FIELD = "loginId";
    private static final String TOKEN_HASH_FIELD = "tokenHash";

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${jwt.refresh-token.redis-key-prefix:auth:refresh-token:}")
    private String keyPrefix;

    @Override
    public void save(UUID tokenId, String loginId, String tokenHash, Duration ttl) {
        String key = buildKey(tokenId);
        stringRedisTemplate.opsForHash().putAll(key, Map.of(
                LOGIN_ID_FIELD, loginId,
                TOKEN_HASH_FIELD, tokenHash
        ));
        stringRedisTemplate.expire(key, ttl);
    }

    @Override
    public Optional<StoredRefreshToken> find(UUID tokenId) {
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(buildKey(tokenId));
        if (entries.isEmpty()) {
            return Optional.empty();
        }

        Object loginId = entries.get(LOGIN_ID_FIELD);
        Object tokenHash = entries.get(TOKEN_HASH_FIELD);
        if (loginId == null || tokenHash == null) {
            return Optional.empty();
        }

        return Optional.of(new StoredRefreshToken(loginId.toString(), tokenHash.toString()));
    }

    @Override
    public void delete(UUID tokenId) {
        stringRedisTemplate.delete(buildKey(tokenId));
    }

    private String buildKey(UUID tokenId) {
        return keyPrefix + tokenId;
    }
}
