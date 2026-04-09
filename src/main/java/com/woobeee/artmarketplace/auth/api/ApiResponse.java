package com.woobeee.artmarketplace.auth.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record ApiResponse<T>(
        Header header,
        T data
) {
    @Builder
    public record Header(
            boolean isSuccessful,
            String message
    ) {}

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(
                new Header(true, message),
                data
        );
    }

    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(
                new Header(true, message),
                null
        );
    }

    public static <T> ApiResponse<T> createSuccess(T data, String message) {
        return new ApiResponse<>(
                new Header(true, message),
                data
        );
    }

    public static <T> ApiResponse<T> createSuccess(String message) {
        return new ApiResponse<>(
                new Header(true, message),
                null
        );
    }

    public static <T> ApiResponse<T> deleteSuccess(T data, String message) {
        return new ApiResponse<>(
                new Header(true, message),
                data
        );
    }

    public static ApiResponse<LocalDateTime> fail(HttpStatus errorCode, String message) {
        return new ApiResponse<>(
                new Header(false, message),
                LocalDateTime.now()
        );
    }
}