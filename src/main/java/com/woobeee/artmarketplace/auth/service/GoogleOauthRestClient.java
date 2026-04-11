package com.woobeee.artmarketplace.auth.service;

import com.woobeee.artmarketplace.auth.config.GoogleOauthProperties;
import com.woobeee.artmarketplace.auth.service.dto.GoogleTokenExchangeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class GoogleOauthRestClient implements GoogleOauthClient {
    private final GoogleOauthProperties googleOauthProperties;
    private final RestClient restClient = RestClient.builder().build();

    @Override
    public GoogleTokenExchangeResponse exchangeAuthorizationCode(String code, String codeVerifier) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("client_id", googleOauthProperties.getClientId());
        form.add("client_secret", googleOauthProperties.getClientSecret());
        form.add("redirect_uri", googleOauthProperties.getRedirectUri());
        form.add("code_verifier", codeVerifier);

        try {
            GoogleTokenExchangeResponse response = restClient.post()
                    .uri(googleOauthProperties.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(GoogleTokenExchangeResponse.class);

            if (response == null || response.idToken() == null || response.idToken().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Google token response is invalid");
            }

            return response;
        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to exchange Google authorization code",
                    exception
            );
        }
    }
}
