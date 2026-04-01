package com.woobeee.auth.exception;

public enum ErrorCode {
    signIn_GoogleTokenNotValid,
    signIn_userConflict,
    login_userNotFound,
    login_passwordNotMatch,
    login_jwtExpired,
    login_jwtInvalid,
    login_refreshTokenMissing,
    login_refreshTokenNotFound,

    comment_needAuthentication,
    like_needAuthentication,
    post_notFound,

    api_idempotencyKeyConflictStopTryingToMessWithMyServer,
    api_idempotencyKeyConflict
}
