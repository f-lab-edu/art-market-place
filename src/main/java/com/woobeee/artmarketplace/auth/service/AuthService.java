package com.woobeee.artmarketplace.auth.service;

import com.woobeee.artmarketplace.auth.api.request.BuyerSignupRequest;
import com.woobeee.artmarketplace.auth.api.request.LoginRequest;
import com.woobeee.artmarketplace.auth.api.request.SellerSignupRequest;
import com.woobeee.artmarketplace.auth.api.response.TokenResponse;
import com.woobeee.artmarketplace.auth.entity.Buyer;
import com.woobeee.artmarketplace.auth.entity.Seller;
import com.woobeee.artmarketplace.auth.repository.BuyerRepository;
import com.woobeee.artmarketplace.auth.repository.SellerRepository;
import com.woobeee.artmarketplace.auth.service.dto.GoogleIdentity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final BuyerRepository buyerRepository;
    private final SellerRepository sellerRepository;
    private final GoogleIdentityVerifier googleIdentityVerifier;
    private final TokenService tokenService;

    @Transactional
    public TokenResponse signupBuyer(BuyerSignupRequest request, String ip) {
        GoogleIdentity identity = googleIdentityVerifier.verify(request.idToken());
        if (buyerRepository.existsByGoogleSubject(identity.subject())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Buyer already exists");
        }

        Buyer buyer = buyerRepository.save(Buyer.create(
                identity.subject(),
                identity.email(),
                request.nickname().trim(),
                request.termsAgreed(),
                request.privacyPolicyAgreed()
        ));

        return createSession(
                buyer.getId(),
                "ROLE_BUYER",
                request.device(),
                ip
        );
    }

    @Transactional
    public TokenResponse signupSeller(SellerSignupRequest request, String ip) {
        GoogleIdentity identity = googleIdentityVerifier.verify(request.idToken());
        if (sellerRepository.existsByGoogleSubject(identity.subject())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seller already exists");
        }

        Seller seller = sellerRepository.save(Seller.create(
                identity.subject(),
                identity.email(),
                request.nickname().trim(),
                request.termsAgreed(),
                request.privacyPolicyAgreed(),
                normalizeOptionalText(request.businessRegistrationCertificateUrl())
        ));

        return createSession(
                seller.getId(),
                "ROLE_SELLER",
                request.device(),
                ip
        );
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request, String ip) {
        GoogleIdentity identity = googleIdentityVerifier.verify(request.idToken());

        return switch (request.memberType()) {
            case BUYER -> buyerRepository.findByGoogleSubject(identity.subject())
                    .filter(Buyer::isActive)
                    .map(buyer -> createSession(
                            buyer.getId(),
                            request.memberType().roleName(),
                            request.device(),
                            ip
                    ))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Buyer is not registered"));
            case SELLER -> sellerRepository.findByGoogleSubject(identity.subject())
                    .filter(Seller::isActive)
                    .map(seller -> createSession(
                            seller.getId(),
                            request.memberType().roleName(),
                            request.device(),
                            ip
                    ))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Seller is not registered"));
        };
    }

    private TokenResponse createSession(
            Long memberId,
            String role,
            String device,
            String ip
    ) {
        return tokenService.issue(memberId, role, device, ip);
    }

    private String normalizeOptionalText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
