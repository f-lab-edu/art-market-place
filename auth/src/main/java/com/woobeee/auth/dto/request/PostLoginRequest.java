package com.woobeee.auth.dto.request;

import lombok.Builder;

@Builder
public record PostLoginRequest (
        String loginId,
        String password
){
}
