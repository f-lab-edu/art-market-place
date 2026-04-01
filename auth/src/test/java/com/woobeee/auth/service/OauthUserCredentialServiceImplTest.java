package com.woobeee.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.json.webtoken.JsonWebSignature;
import com.woobeee.auth.dto.provider.MessageEvent;
import com.woobeee.auth.dto.response.AuthTokenResponse;
import com.woobeee.auth.dto.response.IssuedAuthTokens;
import com.woobeee.auth.entity.Auth;
import com.woobeee.auth.entity.UserAuth;
import com.woobeee.auth.entity.UserCredential;
import com.woobeee.auth.entity.enums.AuthType;
import com.woobeee.auth.exception.UserConflictException;
import com.woobeee.auth.exception.UserNotFoundException;
import com.woobeee.auth.repository.AuthRepository;
import com.woobeee.auth.repository.UserAuthRepository;
import com.woobeee.auth.repository.UserCredentialRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OauthUserCredentialServiceImplTest {
    @Test
    void signIn_withInvalidGoogleToken_throwsRuntimeException() {
        TestOauthUserCredentialService service = service();
        service.googleIdToken = null;

        assertThatThrownBy(() -> service.signIn("invalid"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("signIn.googleTokenNotValid");
    }

    @Test
    void signIn_whenUserAlreadyExists_throwsUserConflictException() {
        TestOauthUserCredentialService service = service();
        service.googleIdToken = googleIdToken("hello@woobeee.com", "subject-1");
        service.userCredentialRepository.existsByLoginId = true;

        assertThatThrownBy(() -> service.signIn("token"))
                .isInstanceOf(UserConflictException.class)
                .hasMessage("signIn_userConflict");
    }

    @Test
    void signIn_success_persistsUserPublishesEventAndIssuesTokens() {
        TestOauthUserCredentialService service = service();
        service.googleIdToken = googleIdToken("hello@woobeee.com", "subject-1");
        service.authRepository.authsByType.put(AuthType.ROLE_MEMBER, new Auth(1L, AuthType.ROLE_MEMBER));
        IssuedAuthTokens expected = new IssuedAuthTokens(
                new AuthTokenResponse("access", "Bearer", 3600L),
                "refresh"
        );
        service.authTokenService.issuedAuthTokens = expected;

        IssuedAuthTokens actual = service.signIn("token");

        assertThat(actual).isEqualTo(expected);
        assertThat(service.passwordEncoder.lastRawPassword).isEqualTo("subject-1");
        assertThat(service.userCredentialRepository.savedCredential.getLoginId()).isEqualTo("hello@woobeee.com");
        assertThat(service.userCredentialRepository.savedCredential.getPassword()).isEqualTo("ENC:subject-1");
        assertThat(service.userAuthRepository.savedUserAuths).hasSize(1);
        assertThat(service.userAuthRepository.savedUserAuths.get(0).getAuthId()).isEqualTo(1L);
        assertThat(service.authTokenService.lastIssueLoginId).isEqualTo("hello@woobeee.com");
        assertThat(service.authTokenService.lastIssueAuthTypes).containsExactly(AuthType.ROLE_MEMBER);
        assertThat(service.eventPublisher.lastEvent).isInstanceOf(MessageEvent.class);

        MessageEvent event = (MessageEvent) service.eventPublisher.lastEvent;
        assertThat(event.topic()).isEqualTo("sign-in-trigger");
        assertThat(event.key()).isEqualTo("hello@woobeee.com");
        ObjectNode payload = (ObjectNode) event.message();
        assertThat(payload.get("loginId").asText()).isEqualTo("hello@woobeee.com");
    }

    @Test
    void logIn_withInvalidGoogleToken_throwsRuntimeException() {
        TestOauthUserCredentialService service = service();
        service.googleIdToken = null;

        assertThatThrownBy(() -> service.logIn("invalid"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("signIn.googleTokenNotValid");
    }

    @Test
    void logIn_whenUserMissing_throwsUserNotFoundException() {
        TestOauthUserCredentialService service = service();
        service.googleIdToken = googleIdToken("hello@woobeee.com", "subject-1");

        assertThatThrownBy(() -> service.logIn("token"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("login_userNotFound");
    }

    @Test
    void logIn_success_issuesRoleMemberToken() {
        TestOauthUserCredentialService service = service();
        service.googleIdToken = googleIdToken("hello@woobeee.com", "subject-1");
        service.userCredentialRepository.byLoginId.put(
                "hello@woobeee.com",
                new UserCredential(UUID.randomUUID(), "hello@woobeee.com", "pw", LocalDateTime.now(), null)
        );
        IssuedAuthTokens expected = new IssuedAuthTokens(
                new AuthTokenResponse("access", "Bearer", 3600L),
                "refresh"
        );
        service.authTokenService.issuedAuthTokens = expected;

        IssuedAuthTokens actual = service.logIn("token");

        assertThat(actual).isEqualTo(expected);
        assertThat(service.authTokenService.lastIssueLoginId).isEqualTo("hello@woobeee.com");
        assertThat(service.authTokenService.lastIssueAuthTypes).containsExactly(AuthType.ROLE_MEMBER);
    }

    @Test
    void refresh_delegatesToAuthTokenService() {
        TestOauthUserCredentialService service = service();
        IssuedAuthTokens expected = new IssuedAuthTokens(
                new AuthTokenResponse("access", "Bearer", 3600L),
                "refresh"
        );
        service.authTokenService.refreshedAuthTokens = expected;

        IssuedAuthTokens actual = service.refresh("refresh-token");

        assertThat(actual).isEqualTo(expected);
        assertThat(service.authTokenService.lastRefreshToken).isEqualTo("refresh-token");
    }

    @Test
    void logout_delegatesToAuthTokenService() {
        TestOauthUserCredentialService service = service();

        service.logout("refresh-token");

        assertThat(service.authTokenService.lastRevokedToken).isEqualTo("refresh-token");
    }

    private static TestOauthUserCredentialService service() {
        AuthRepositoryHandler authRepository = new AuthRepositoryHandler();
        UserAuthRepositoryHandler userAuthRepository = new UserAuthRepositoryHandler();
        UserCredentialRepositoryHandler userCredentialRepository = new UserCredentialRepositoryHandler();
        FakeAuthTokenService authTokenService = new FakeAuthTokenService();
        FakePasswordEncoder passwordEncoder = new FakePasswordEncoder();
        FakeEventPublisher eventPublisher = new FakeEventPublisher();
        return new TestOauthUserCredentialService(
                authRepository,
                userAuthRepository,
                userCredentialRepository,
                authTokenService,
                passwordEncoder,
                eventPublisher
        );
    }

    private static GoogleIdToken googleIdToken(String email, String subject) {
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload()
                .setEmail(email)
                .setSubject(subject);
        return new GoogleIdToken(new JsonWebSignature.Header(), payload, new byte[0], new byte[0]);
    }

    private static final class TestOauthUserCredentialService extends OauthUserCredentialServiceImpl {
        private GoogleIdToken googleIdToken;
        private final AuthRepositoryHandler authRepository;
        private final UserAuthRepositoryHandler userAuthRepository;
        private final UserCredentialRepositoryHandler userCredentialRepository;
        private final FakeAuthTokenService authTokenService;
        private final FakePasswordEncoder passwordEncoder;
        private final FakeEventPublisher eventPublisher;

        private TestOauthUserCredentialService(
                AuthRepositoryHandler authRepository,
                UserAuthRepositoryHandler userAuthRepository,
                UserCredentialRepositoryHandler userCredentialRepository,
                FakeAuthTokenService authTokenService,
                FakePasswordEncoder passwordEncoder,
                FakeEventPublisher eventPublisher
        ) {
            super(
                    (GoogleIdTokenVerifier) null,
                    authRepository.repository(),
                    userAuthRepository.repository(),
                    userCredentialRepository.repository(),
                    authTokenService,
                    passwordEncoder,
                    eventPublisher,
                    new ObjectMapper()
            );
            this.authRepository = authRepository;
            this.userAuthRepository = userAuthRepository;
            this.userCredentialRepository = userCredentialRepository;
            this.authTokenService = authTokenService;
            this.passwordEncoder = passwordEncoder;
            this.eventPublisher = eventPublisher;
        }

        @Override
        public GoogleIdToken verifyIdToken(String idTokenString) {
            return googleIdToken;
        }
    }

    private static final class FakeAuthTokenService implements AuthTokenService {
        private IssuedAuthTokens issuedAuthTokens;
        private IssuedAuthTokens refreshedAuthTokens;
        private String lastIssueLoginId;
        private List<AuthType> lastIssueAuthTypes;
        private String lastRefreshToken;
        private String lastRevokedToken;

        @Override
        public IssuedAuthTokens issueTokens(String loginId, List<AuthType> authTypes) {
            this.lastIssueLoginId = loginId;
            this.lastIssueAuthTypes = authTypes;
            return issuedAuthTokens;
        }

        @Override
        public IssuedAuthTokens refresh(String refreshToken) {
            this.lastRefreshToken = refreshToken;
            return refreshedAuthTokens;
        }

        @Override
        public void revoke(String refreshToken) {
            this.lastRevokedToken = refreshToken;
        }
    }

    private static final class FakePasswordEncoder implements PasswordEncoder {
        private String lastRawPassword;

        @Override
        public String encode(CharSequence rawPassword) {
            this.lastRawPassword = rawPassword.toString();
            return "ENC:" + rawPassword;
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            return encodedPassword.equals("ENC:" + rawPassword);
        }

        @Override
        public boolean upgradeEncoding(String encodedPassword) {
            return false;
        }
    }

    private static final class FakeEventPublisher implements ApplicationEventPublisher {
        private Object lastEvent;

        @Override
        public void publishEvent(Object event) {
            this.lastEvent = event;
        }
    }

    private static final class UserCredentialRepositoryHandler implements InvocationHandler {
        private final Map<String, UserCredential> byLoginId = new HashMap<>();
        private boolean existsByLoginId;
        private UserCredential savedCredential;

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
                case "existsByLoginId" -> existsByLoginId;
                case "save" -> save((UserCredential) args[0]);
                case "findUserCredentialByLoginId" -> Optional.ofNullable(byLoginId.get(args[0]));
                case "toString" -> "UserCredentialRepositoryProxy";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }

        private UserCredential save(UserCredential credential) {
            UUID savedId = credential.getId() != null ? credential.getId() : UUID.randomUUID();
            savedCredential = new UserCredential(savedId, credential.getLoginId(), credential.getPassword(), credential.getCreatedAt(), credential.getUpdatedAt());
            byLoginId.put(savedCredential.getLoginId(), savedCredential);
            return savedCredential;
        }
    }

    private static final class UserAuthRepositoryHandler implements InvocationHandler {
        private final List<UserAuth> savedUserAuths = new ArrayList<>();

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
                case "saveAll" -> saveAll((Iterable<UserAuth>) args[0]);
                case "toString" -> "UserAuthRepositoryProxy";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }

        private List<UserAuth> saveAll(Iterable<UserAuth> userAuths) {
            for (UserAuth userAuth : userAuths) {
                savedUserAuths.add(userAuth);
            }
            return savedUserAuths;
        }
    }

    private static final class AuthRepositoryHandler implements InvocationHandler {
        private final Map<AuthType, Auth> authsByType = new HashMap<>();

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
                case "findAllByAuthTypeIn" -> findAllByAuthTypeIn((List<AuthType>) args[0]);
                case "toString" -> "AuthRepositoryProxy";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }

        private List<Auth> findAllByAuthTypeIn(List<AuthType> authTypes) {
            List<Auth> result = new ArrayList<>();
            for (AuthType authType : authTypes) {
                Auth auth = authsByType.get(authType);
                if (auth != null) {
                    result.add(auth);
                }
            }
            return result;
        }
    }
}
