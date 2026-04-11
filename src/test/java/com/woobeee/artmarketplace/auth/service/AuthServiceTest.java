package com.woobeee.artmarketplace.auth.service;

import com.woobeee.artmarketplace.auth.api.request.BuyerSignupRequest;
import com.woobeee.artmarketplace.auth.api.request.LoginRequest;
import com.woobeee.artmarketplace.auth.api.request.SellerSignupRequest;
import com.woobeee.artmarketplace.auth.api.response.TokenResponse;
import com.woobeee.artmarketplace.auth.entity.Buyer;
import com.woobeee.artmarketplace.auth.entity.MemberType;
import com.woobeee.artmarketplace.auth.entity.Seller;
import com.woobeee.artmarketplace.auth.repository.BuyerRepository;
import com.woobeee.artmarketplace.auth.repository.SellerRepository;
import com.woobeee.artmarketplace.auth.service.dto.GoogleIdentity;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private BuyerRepository buyerRepository;

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private GoogleIdentityVerifier googleIdentityVerifier;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    void signupBuyerCreatesBuyerAndIssuesTokens() {
        BuyerSignupRequest request = new BuyerSignupRequest("id-token", "buyer-nick", true, true, "ios");
        when(googleIdentityVerifier.verify("id-token"))
                .thenReturn(new GoogleIdentity("google-sub", "buyer@example.com", "Buyer Name"));
        when(buyerRepository.existsByGoogleSubject("google-sub")).thenReturn(false);
        when(buyerRepository.save(any(Buyer.class))).thenAnswer(invocation -> {
            Buyer buyer = invocation.getArgument(0);
            ReflectionTestUtils.setField(buyer, "id", 11L);
            return buyer;
        });
        when(tokenService.issue(11L, "ROLE_BUYER", "ios", "127.0.0.1"))
                .thenReturn(new TokenResponse("access", 900, "refresh", 2_592_000));

        TokenResponse response = authService.signupBuyer(request, "127.0.0.1");

        ArgumentCaptor<Buyer> buyerCaptor = ArgumentCaptor.forClass(Buyer.class);
        verify(buyerRepository).save(buyerCaptor.capture());
        Buyer savedBuyer = buyerCaptor.getValue();

        assertThat(savedBuyer.getGoogleSubject()).isEqualTo("google-sub");
        assertThat(savedBuyer.getEmail()).isEqualTo("buyer@example.com");
        assertThat(savedBuyer.getNickname()).isEqualTo("buyer-nick");
        assertThat(savedBuyer.isTermsAgreed()).isTrue();
        assertThat(savedBuyer.isPrivacyPolicyAgreed()).isTrue();
        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
    }

    @Test
    void signupSellerCreatesSellerAndIssuesTokens() {
        SellerSignupRequest request = new SellerSignupRequest(
                "id-token",
                "seller-nick",
                true,
                true,
                "https://cdn.example.com/cert.png",
                "web"
        );
        when(googleIdentityVerifier.verify("id-token"))
                .thenReturn(new GoogleIdentity("seller-sub", "seller@example.com", "Seller Name"));
        when(sellerRepository.existsByGoogleSubject("seller-sub")).thenReturn(false);
        when(sellerRepository.save(any(Seller.class))).thenAnswer(invocation -> {
            Seller seller = invocation.getArgument(0);
            ReflectionTestUtils.setField(seller, "id", 29L);
            return seller;
        });
        when(tokenService.issue(29L, "ROLE_SELLER", "web", "127.0.0.1"))
                .thenReturn(new TokenResponse("seller-access", 900, "seller-refresh", 2_592_000));

        TokenResponse response = authService.signupSeller(request, "127.0.0.1");

        ArgumentCaptor<Seller> sellerCaptor = ArgumentCaptor.forClass(Seller.class);
        verify(sellerRepository).save(sellerCaptor.capture());
        Seller savedSeller = sellerCaptor.getValue();

        assertThat(savedSeller.getGoogleSubject()).isEqualTo("seller-sub");
        assertThat(savedSeller.getBusinessRegistrationCertificateUrl())
                .isEqualTo("https://cdn.example.com/cert.png");
        assertThat(response.accessToken()).isEqualTo("seller-access");
        assertThat(response.refreshToken()).isEqualTo("seller-refresh");
    }

    @Test
    void loginBuyerIssuesTokensForExistingMember() {
        Buyer buyer = Buyer.create("buyer-sub", "buyer@example.com", "buyer-nick", true, true);
        ReflectionTestUtils.setField(buyer, "id", 77L);

        when(googleIdentityVerifier.verify("id-token"))
                .thenReturn(new GoogleIdentity("buyer-sub", "buyer@example.com", "Buyer Name"));
        when(buyerRepository.findByGoogleSubject("buyer-sub")).thenReturn(Optional.of(buyer));
        when(tokenService.issue(77L, "ROLE_BUYER", "android", "10.0.0.5"))
                .thenReturn(new TokenResponse("access-77", 900, "refresh-77", 2_592_000));

        TokenResponse response = authService.login(
                new LoginRequest("id-token", MemberType.BUYER, "android"),
                "10.0.0.5"
        );

        assertThat(response.accessToken()).isEqualTo("access-77");
        assertThat(response.refreshToken()).isEqualTo("refresh-77");
    }

    @Test
    void loginFailsWhenSellerIsNotRegistered() {
        when(googleIdentityVerifier.verify("id-token"))
                .thenReturn(new GoogleIdentity("missing-seller", "seller@example.com", "Seller Name"));
        when(sellerRepository.findByGoogleSubject("missing-seller")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("id-token", MemberType.SELLER, "web"),
                "127.0.0.1"
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND")
                .hasMessageContaining("Seller is not registered");
    }
}
