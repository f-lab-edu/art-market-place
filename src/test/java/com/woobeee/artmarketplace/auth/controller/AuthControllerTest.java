package com.woobeee.artmarketplace.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.woobeee.artmarketplace.auth.api.request.BuyerSignupRequest;
import com.woobeee.artmarketplace.auth.api.request.LoginRequest;
import com.woobeee.artmarketplace.auth.api.response.TokenResponse;
import com.woobeee.artmarketplace.auth.entity.MemberType;
import com.woobeee.artmarketplace.auth.exception.AuthRestControllerAdvice;
import com.woobeee.artmarketplace.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(AuthRestControllerAdvice.class)
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @Test
    void signupBuyerReturnsTokenResponse() throws Exception {
        BuyerSignupRequest request = new BuyerSignupRequest("id-token", "buyer-nick", true, true, "ios");
        TokenResponse tokenResponse = new TokenResponse("access-token", 900, "refresh-token", 2_592_000);
        when(authService.signupBuyer(eq(request), eq("203.0.113.10"))).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/auth/signup/buyers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "203.0.113.10, 198.51.100.1")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.isSuccessful").value(true))
                .andExpect(jsonPath("$.header.message").value("Buyer signup completed"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));

        verify(authService).signupBuyer(request, "203.0.113.10");
    }

    @Test
    void loginReturnsTokenResponse() throws Exception {
        LoginRequest request = new LoginRequest("id-token", MemberType.BUYER, "android");
        TokenResponse tokenResponse = new TokenResponse("access-77", 900, "refresh-77", 2_592_000);
        when(authService.login(eq(request), eq("10.0.0.5"))).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Real-IP", "10.0.0.5")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.header.isSuccessful").value(true))
                .andExpect(jsonPath("$.header.message").value("Login completed"))
                .andExpect(jsonPath("$.data.accessToken").value("access-77"))
                .andExpect(jsonPath("$.data.refreshTokenExpiresInSeconds").value(2_592_000));

        verify(authService).login(request, "10.0.0.5");
    }

    @Test
    void signupBuyerRejectsInvalidRequestBody() throws Exception {
        BuyerSignupRequest request = new BuyerSignupRequest("id-token", " ", true, true, "ios");

        mockMvc.perform(post("/api/auth/signup/buyers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.header.isSuccessful").value(false))
                .andExpect(jsonPath("$.header.message").value("Nickname is required"));

        verifyNoInteractions(authService);
    }
}
