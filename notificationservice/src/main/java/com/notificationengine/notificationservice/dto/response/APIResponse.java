package com.notificationengine.notificationservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class APIResponse<T> {

    private boolean success;
    private int statusCode;
    private String message;
    private LocalDateTime timestamp;
    private T data;
    private ErrorDetails error;

    public static <T> APIResponse<T> success(int statusCode, String message, T data) {
        return APIResponse.<T>builder()
                .success(true)
                .statusCode(statusCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .data(data)
                .build();
    }

    public static <T> APIResponse<T> failure(int statusCode, String message, String errorDetails) {
        return APIResponse.<T>builder()
                .success(false)
                .statusCode(statusCode)
                .message(message)
                .timestamp(LocalDateTime.now())
                .error(new ErrorDetails(message, errorDetails))
                .build();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ErrorDetails {
        private String errorType;
        private String detailedMessage;
    }
}