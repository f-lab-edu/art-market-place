package com.woobeee.artmarketplace.auth.service.dto;

import com.woobeee.artmarketplace.auth.entity.MemberType;

public record GoogleAuthorizationContext(
        GoogleAuthorizationAction action,
        String codeVerifier,
        String device,
        MemberType memberType,
        String nickname,
        boolean termsAgreed,
        boolean privacyPolicyAgreed,
        String businessRegistrationCertificateUrl
) {
}
