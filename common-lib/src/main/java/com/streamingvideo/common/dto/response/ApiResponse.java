package com.streamingvideo.common.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.streamingvideo.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;
    private Object details;
    private String path;
    private Instant timestamp;

    private static <T> ApiResponseBuilder<T> baseBuilder(int code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .timestamp(Instant.now());
    }

    public static <T> ApiResponse<T> success() {
        return ApiResponse.<T>baseBuilder(HttpStatus.OK.value(), "Success")
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>baseBuilder(HttpStatus.OK.value(), "Success")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>baseBuilder(HttpStatus.OK.value(), message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> created() {
        return ApiResponse.<T>baseBuilder(HttpStatus.CREATED.value(), "Created")
                .build();
    }

    public static <T> ApiResponse<T> created(T data) {
        return ApiResponse.<T>baseBuilder(HttpStatus.CREATED.value(), "Created")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return ApiResponse.<T>baseBuilder(HttpStatus.CREATED.value(), message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(int code) {
        return ApiResponse.<T>baseBuilder(code, "Error")
                .build();
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>baseBuilder(code, message)
                .build();
    }

    public static <T> ApiResponse<T> error(int code, String message, T data) {
        return ApiResponse.<T>baseBuilder(code, message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(int code, String message, Object details, String path) {
        return ApiResponse.<T>baseBuilder(code, message)
                .details(details)
                .path(path)
                .build();
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return ApiResponse.<T>baseBuilder(errorCode.getCode(), errorCode.getMessage())
                .build();
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, T data) {
        return ApiResponse.<T>baseBuilder(errorCode.getCode(), errorCode.getMessage())
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, Object details, String path) {
        return ApiResponse.<T>baseBuilder(errorCode.getCode(), errorCode.getMessage())
                .details(details)
                .path(path)
                .build();
    }

    public static <T> ApiResponse<T> badRequest() {
        return ApiResponse.<T>baseBuilder(HttpStatus.BAD_REQUEST.value(), "Bad Request Error")
                .build();
    }

    public static <T> ApiResponse<T> badRequest(String message) {
        return ApiResponse.<T>baseBuilder(HttpStatus.BAD_REQUEST.value(), message)
                .build();
    }

    public static <T> ApiResponse<T> internalServerError() {
        return ApiResponse.<T>baseBuilder(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error")
                .build();
    }

    public static <T> ApiResponse<T> internalServerError(String message) {
        return ApiResponse.<T>baseBuilder(HttpStatus.INTERNAL_SERVER_ERROR.value(), message)
                .build();
    }

    public static <T> ApiResponse<T> internalServerError(String message, T data) {
        return ApiResponse.<T>baseBuilder(HttpStatus.INTERNAL_SERVER_ERROR.value(), message)
                .data(data)
                .build();
    }
}