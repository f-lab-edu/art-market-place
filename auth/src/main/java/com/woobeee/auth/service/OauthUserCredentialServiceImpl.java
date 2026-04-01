package com.woobeee.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.woobeee.auth.dto.provider.MessageEvent;
import com.woobeee.auth.dto.response.IssuedAuthTokens;
import com.woobeee.auth.entity.Auth;
import com.woobeee.auth.entity.UserAuth;
import com.woobeee.auth.entity.UserCredential;
import com.woobeee.auth.entity.enums.AuthType;
import com.woobeee.auth.exception.ErrorCode;
import com.woobeee.auth.exception.UserConflictException;
import com.woobeee.auth.exception.UserNotFoundException;
import com.woobeee.auth.repository.AuthRepository;
import com.woobeee.auth.repository.UserAuthRepository;
import com.woobeee.auth.repository.UserCredentialRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class OauthUserCredentialServiceImpl implements OauthUserCredentialService{
    private final GoogleIdTokenVerifier verifier;
    private final AuthRepository authRepository;
    private final UserAuthRepository userAuthRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final AuthTokenService authTokenService;
    private final PasswordEncoder passwordEncoder;

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Override
    public IssuedAuthTokens signIn(String idTokenString) {
        GoogleIdToken idToken = verifyIdToken(idTokenString);
        if (idToken == null) {
            throw new RuntimeException("signIn.googleTokenNotValid");
        }

        String email = idToken.getPayload().getEmail();
        String userUuid = idToken.getPayload().getSubject();

        if (userCredentialRepository.existsByLoginId(email)) {
            throw new UserConflictException(ErrorCode.signIn_userConflict);
        }

        List<Auth> auths = authRepository
                .findAllByAuthTypeIn(
                        List.of(AuthType.ROLE_MEMBER)
                );

        UserCredential userCredential = UserCredential.builder()
                .loginId(email)
                .password(passwordEncoder.encode(userUuid))
                .build();

        UserCredential savedUserCredential = userCredentialRepository.save(userCredential);

        List<UserAuth> userAuths = auths.stream()
                .map(auth -> UserAuth.builder()
                        .id(new UserAuth.UserAuthId(savedUserCredential.getId(), auth.getId()))
                        .build())
                .toList();

        userAuthRepository.saveAll(userAuths);

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("id",
                String.valueOf(savedUserCredential.getId()));
        payload.put("loginId",
                savedUserCredential.getLoginId());


        MessageEvent event = MessageEvent.builder()
                .eventId(UUID.randomUUID())
                .topic("sign-in-trigger")
                .key(savedUserCredential.getLoginId())
                .message(payload)
                .build();

        eventPublisher.publishEvent(event);

        return authTokenService.issueTokens(email, List.of(AuthType.ROLE_MEMBER));
    }

    @Override
    public IssuedAuthTokens logIn(String idTokenString) {
        GoogleIdToken idToken = verifyIdToken(idTokenString);

        if (idToken == null) {
            throw new RuntimeException("signIn.googleTokenNotValid");
        }

        String email = idToken.getPayload().getEmail();
        userCredentialRepository.findUserCredentialByLoginId(email)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.login_userNotFound));

        return authTokenService.issueTokens(email, List.of(AuthType.ROLE_MEMBER));
    }

    @Override
    public IssuedAuthTokens refresh(String refreshToken) {
        return authTokenService.refresh(refreshToken);
    }

    @Override
    public void logout(String refreshToken) {
        authTokenService.revoke(refreshToken);
    }


    public GoogleIdToken verifyIdToken(String idTokenString) {
        try {
            return verifier.verify(idTokenString);
        } catch (Exception e) {
            return null;
        }
    }
}
