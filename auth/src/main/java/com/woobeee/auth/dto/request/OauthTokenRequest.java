package com.woobeee.auth.dto.request;

import lombok.Builder;

@Builder
public record OauthTokenRequest (String idToken){
}
