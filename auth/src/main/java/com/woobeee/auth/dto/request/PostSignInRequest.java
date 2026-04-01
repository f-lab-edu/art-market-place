package com.woobeee.auth.dto.request;

import com.woobeee.auth.entity.enums.AuthType;
import lombok.Builder;

import java.util.List;

@Builder
public record PostSignInRequest (
        String loginId,
        String password,
        String nickname,
        List<AuthType> authTypes
){
}
