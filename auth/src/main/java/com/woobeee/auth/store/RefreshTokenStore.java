package com.woobeee.auth.store;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenStore {
    void save(UUID tokenId, String loginId, String tokenHash, Duration ttl);
    Optional<StoredRefreshToken> find(UUID tokenId);
    void delete(UUID tokenId);

    record StoredRefreshToken(
            String loginId,
            String tokenHash
    ) {
    }
}
