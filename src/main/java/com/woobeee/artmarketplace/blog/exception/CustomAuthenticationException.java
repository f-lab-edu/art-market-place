package com.woobeee.artmarketplace.blog.exception;

public class CustomAuthenticationException extends RuntimeException{
    public CustomAuthenticationException(ErrorCode message) {
        super(message.name());
    }
}
