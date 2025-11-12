package com.oms.orders.common.utils;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorResponse {
    private boolean success;
    private String errorCode;
    private String message;
    private Instant timestamp;

    public static ApiErrorResponse of(String code, String message) {
        return ApiErrorResponse.builder()
                .success(false)
                .errorCode(code)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }
}