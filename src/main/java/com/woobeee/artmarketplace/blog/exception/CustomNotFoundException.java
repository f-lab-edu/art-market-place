package com.woobeee.artmarketplace.blog.exception;

public class CustomNotFoundException extends RuntimeException {
    public CustomNotFoundException(ErrorCode message) {
        super(message.name());
    }
}
