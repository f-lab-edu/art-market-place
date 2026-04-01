package com.woobeee.auth.service;

import com.woobeee.auth.dto.response.IssuedAuthTokens;
import com.woobeee.auth.entity.Auth;
import com.woobeee.auth.entity.UserAuth;
import com.woobeee.auth.entity.UserCredential;
import com.woobeee.auth.entity.enums.AuthType;
import com.woobeee.auth.exception.JwtNotValidException;
import com.woobeee.auth.repository.AuthRepository;
import com.woobeee.auth.repository.UserAuthRepository;
import com.woobeee.auth.repository.UserCredentialRepository;
import com.woobeee.auth.store.RefreshTokenStore;
import com.woobeee.auth.token.AccessTokenProvider;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthTokenServiceImplTest {
    private static final String ACCESS_SECRET = "01234567890123456789012345678901";
    private static final String REFRESH_SECRET = "abcdefghijklmnopqrstuvwxyz123456";

    @Test
    void issueTokens_savesRefreshTokenAndReturnsBearerResponse() {
        InMemoryRefreshTokenStore refreshTokenStore = new InMemoryRefreshTokenStore();
        AuthTokenServiceImpl service = service(refreshTokenStore, new UserCredentialRepositoryHandler(), new UserAuthRepositoryHandler(), new AuthRepositoryHandler());

        IssuedAuthTokens issued = service.issueTokens("helloJosh", List.of(AuthType.ROLE_MEMBER, AuthType.ROLE_ADMIN));

        assertThat(issued.response().tokenType()).isEqualTo("Bearer");
        assertThat(issued.response().expiresIn()).isEqualTo(3_600_000L);
        assertThat(issued.refreshToken()).isNotBlank();
        assertThat(refreshTokenStore.tokens).hasSize(1);
        RefreshTokenStore.StoredRefreshToken stored = refreshTokenStore.tokens.values().iterator().next();
        assertThat(stored.loginId()).isEqualTo("helloJosh");
        assertThat(stored.tokenHash()).isEqualTo(hash(issued.refreshToken()));
        assertThat(refreshTokenStore.lastTtl).isEqualTo(Duration.ofMillis(86_400_000L));
    }

    @Test
    void refresh_withBlankToken_throwsMissingException() {
        AuthTokenServiceImpl service = service(new InMemoryRefreshTokenStore(), new UserCredentialRepositoryHandler(), new UserAuthRepositoryHandler(), new AuthRepositoryHandler());

        assertThatThrownBy(() -> service.refresh(" "))
                .isInstanceOf(JwtNotValidException.class)
                .hasMessage("login_refreshTokenMissing");
    }

    @Test
    void refresh_whenTokenNotStored_throwsNotFoundException() {
        InMemoryRefreshTokenStore refreshTokenStore = new InMemoryRefreshTokenStore();
        AuthTokenServiceImpl service = service(refreshTokenStore, new UserCredentialRepositoryHandler(), new UserAuthRepositoryHandler(), new AuthRepositoryHandler());
        String refreshToken = provider().generateRefreshToken("helloJosh", UUID.randomUUID());

        assertThatThrownBy(() -> service.refresh(refreshToken))
                .isInstanceOf(JwtNotValidException.class)
                .hasMessage("login_refreshTokenNotFound");
    }

    @Test
    void refresh_whenStoredHashDoesNotMatch_deletesTokenAndThrowsInvalid() {
        InMemoryRefreshTokenStore refreshTokenStore = new InMemoryRefreshTokenStore();
        AuthTokenServiceImpl service = service(refreshTokenStore, new UserCredentialRepositoryHandler(), new UserAuthRepositoryHandler(), new AuthRepositoryHandler());

        UUID tokenId = UUID.randomUUID();
        String refreshToken = provider().generateRefreshToken("helloJosh", tokenId);
        refreshTokenStore.save(tokenId, "helloJosh", "wrong-hash", Duration.ofDays(1));

        assertThatThrownBy(() -> service.refresh(refreshToken))
                .isInstanceOf(JwtNotValidException.class)
                .hasMessage("login_jwtInvalid");
        assertThat(refreshTokenStore.deletedTokenIds).contains(tokenId);
        assertThat(refreshTokenStore.tokens).doesNotContainKey(tokenId);
    }

    @Test
    void refresh_whenStoredTokenIsValid_reissuesTokensUsingResolvedAuths() {
        InMemoryRefreshTokenStore refreshTokenStore = new InMemoryRefreshTokenStore();
        UserCredentialRepositoryHandler userCredentialRepository = new UserCredentialRepositoryHandler();
        UserAuthRepositoryHandler userAuthRepository = new UserAuthRepositoryHandler();
        AuthRepositoryHandler authRepository = new AuthRepositoryHandler();
        AuthTokenServiceImpl service = service(refreshTokenStore, userCredentialRepository, userAuthRepository, authRepository);

        UUID userId = UUID.randomUUID();
        UUID refreshTokenId = UUID.randomUUID();
        String refreshToken = provider().generateRefreshToken("helloJosh", refreshTokenId);

        refreshTokenStore.save(refreshTokenId, "helloJosh", hash(refreshToken), Duration.ofDays(1));
        userCredentialRepository.byLoginId.put("helloJosh", new UserCredential(userId, "helloJosh", "pw", LocalDateTime.now(), null));
        userAuthRepository.byUserId.put(userId, List.of(
                new UserAuth(new UserAuth.UserAuthId(userId, 1L), LocalDateTime.now()),
                new UserAuth(new UserAuth.UserAuthId(userId, 2L), LocalDateTime.now())
        ));
        authRepository.byId.put(1L, new Auth(1L, AuthType.ROLE_MEMBER));
        authRepository.byId.put(2L, new Auth(2L, AuthType.ROLE_ADMIN));

        IssuedAuthTokens refreshed = service.refresh(refreshToken);

        assertThat(refreshed.response().tokenType()).isEqualTo("Bearer");
        assertThat(refreshed.refreshToken()).isNotEqualTo(refreshToken);
        assertThat(refreshTokenStore.deletedTokenIds).contains(refreshTokenId);
        assertThat(refreshTokenStore.tokens).hasSize(1);
    }

    @Test
    void revoke_withValidToken_deletesStoredToken() {
        InMemoryRefreshTokenStore refreshTokenStore = new InMemoryRefreshTokenStore();
        AuthTokenServiceImpl service = service(refreshTokenStore, new UserCredentialRepositoryHandler(), new UserAuthRepositoryHandler(), new AuthRepositoryHandler());
        UUID tokenId = UUID.randomUUID();
        String refreshToken = provider().generateRefreshToken("helloJosh", tokenId);

        refreshTokenStore.save(tokenId, "helloJosh", hash(refreshToken), Duration.ofDays(1));
        service.revoke(refreshToken);

        assertThat(refreshTokenStore.deletedTokenIds).contains(tokenId);
        assertThat(refreshTokenStore.tokens).doesNotContainKey(tokenId);
    }

    @Test
    void revoke_withInvalidToken_doesNothing() {
        InMemoryRefreshTokenStore refreshTokenStore = new InMemoryRefreshTokenStore();
        AuthTokenServiceImpl service = service(refreshTokenStore, new UserCredentialRepositoryHandler(), new UserAuthRepositoryHandler(), new AuthRepositoryHandler());

        service.revoke("not-a-token");

        assertThat(refreshTokenStore.deletedTokenIds).isEmpty();
    }

    private static AuthTokenServiceImpl service(
            InMemoryRefreshTokenStore refreshTokenStore,
            UserCredentialRepositoryHandler userCredentialRepository,
            UserAuthRepositoryHandler userAuthRepository,
            AuthRepositoryHandler authRepository
    ) {
        return new AuthTokenServiceImpl(
                provider(),
                refreshTokenStore,
                userCredentialRepository.repository(),
                userAuthRepository.repository(),
                authRepository.repository()
        );
    }

    private static AccessTokenProvider provider() {
        return new AccessTokenProvider(ACCESS_SECRET, REFRESH_SECRET, 3_600_000L, 86_400_000L);
    }

    private static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static final class InMemoryRefreshTokenStore implements RefreshTokenStore {
        private final Map<UUID, StoredRefreshToken> tokens = new HashMap<>();
        private final List<UUID> deletedTokenIds = new ArrayList<>();
        private Duration lastTtl;

        @Override
        public void save(UUID tokenId, String loginId, String tokenHash, Duration ttl) {
            tokens.put(tokenId, new StoredRefreshToken(loginId, tokenHash));
            lastTtl = ttl;
        }

        @Override
        public Optional<StoredRefreshToken> find(UUID tokenId) {
            return Optional.ofNullable(tokens.get(tokenId));
        }

        @Override
        public void delete(UUID tokenId) {
            deletedTokenIds.add(tokenId);
            tokens.remove(tokenId);
        }
    }

    private static final class UserCredentialRepositoryHandler implements InvocationHandler {
        private final Map<String, UserCredential> byLoginId = new HashMap<>();

        private UserCredentialRepository repository() {
            return (UserCredentialRepository) Proxy.newProxyInstance(
                    UserCredentialRepository.class.getClassLoader(),
                    new Class<?>[]{UserCredentialRepository.class},
                    this
            );
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "findUserCredentialByLoginId" -> Optional.ofNullable(byLoginId.get(args[0]));
                case "toString" -> "UserCredentialRepositoryProxy";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }
    }

    private static final class UserAuthRepositoryHandler implements InvocationHandler {
        private final Map<UUID, List<UserAuth>> byUserId = new HashMap<>();

        private UserAuthRepository repository() {
            return (UserAuthRepository) Proxy.newProxyInstance(
                    UserAuthRepository.class.getClassLoader(),
                    new Class<?>[]{UserAuthRepository.class},
                    this
            );
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "findAllById_UserId" -> byUserId.getOrDefault(args[0], List.of());
                case "toString" -> "UserAuthRepositoryProxy";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }
    }

    private static final class AuthRepositoryHandler implements InvocationHandler {
        private final Map<Long, Auth> byId = new HashMap<>();

        private AuthRepository repository() {
            return (AuthRepository) Proxy.newProxyInstance(
                    AuthRepository.class.getClassLoader(),
                    new Class<?>[]{AuthRepository.class},
                    this
            );
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "findAllById" -> ((Iterable<?>) args[0]).iterator().hasNext()
                        ? collectByIds((Iterable<?>) args[0])
                        : List.of();
                case "toString" -> "AuthRepositoryProxy";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }

        private List<Auth> collectByIds(Iterable<?> ids) {
            List<Auth> auths = new ArrayList<>();
            for (Object id : ids) {
                Auth auth = byId.get(id);
                if (auth != null) {
                    auths.add(auth);
                }
            }
            return auths;
        }
    }
}
