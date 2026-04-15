package com.streamingvideo.common.exception;

import com.streamingvideo.common.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * GlobalExceptionHandler dùng chung cho tất cả microservices.
 *
 * Xử lý tập trung:
 * - AppException (business errors với ErrorCode)
 * - Validation errors (MethodArgumentNotValidException)
 * - Missing headers (X-User-Id từ Gateway)
 * - File upload size exceeded
 * - Các lỗi chung khác
 *
 * Services import common-lib sẽ tự động có handler này
 * nhờ @RestControllerAdvice + component scan.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

        /**
         * Xử lý AppException - Business logic errors.
         * Đây là exception chính mà tất cả services throw khi có lỗi nghiệp vụ.
         * Ví dụ: throw new AppException(ErrorCode.USER_NOT_FOUND)
         */
        @ExceptionHandler(AppException.class)
        public ResponseEntity<ApiResponse<Object>> handleAppException(
                        AppException ex, HttpServletRequest request) {

                ErrorCode errorCode = ex.getErrorCode();
                log.warn("AppException: code={}, message={}, path={}",
                                errorCode.getCode(), errorCode.getMessage(), request.getRequestURI());

                ApiResponse<Object> response = ApiResponse.error(
                                errorCode.getCode(),
                                errorCode.getMessage(),
                                ex.getDetails(),
                                request.getRequestURI());

                return ResponseEntity
                                .status(errorCode.getStatusCode())
                                .body(response);
        }

        /**
         * Xử lý Validation errors - Khi @Valid thất bại.
         * Trả về danh sách chi tiết từng field bị lỗi.
         * Ví dụ: username = "" → { field: "username", message: "không được để trống" }
         */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponse<List<ErrorValidResponse>>> handleValidationException(
                        MethodArgumentNotValidException ex, HttpServletRequest request) {

                List<ErrorValidResponse> errors = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(fieldError -> ErrorValidResponse.builder()
                                                .field(fieldError.getField())
                                                .message(fieldError.getDefaultMessage())
                                                .rejectedValue(fieldError.getRejectedValue())
                                                .build())
                                .toList();

                log.warn("Validation failed: {} errors, path={}", errors.size(), request.getRequestURI());

                ApiResponse<List<ErrorValidResponse>> response = ApiResponse.error(
                                ErrorCode.VALIDATION_ERROR.getCode(),
                                ErrorCode.VALIDATION_ERROR.getMessage(),
                                errors);

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(response);
        }

        /**
         * Xử lý thiếu header bắt buộc - Thường là X-User-Id từ Gateway.
         */
        @ExceptionHandler(MissingRequestHeaderException.class)
        public ResponseEntity<ApiResponse<Object>> handleMissingHeader(
                        MissingRequestHeaderException ex, HttpServletRequest request) {

                log.warn("Missing header: {}, path={}", ex.getHeaderName(), request.getRequestURI());

                // Nếu thiếu X-User-Id → Unauthenticated
                if ("X-User-Id".equalsIgnoreCase(ex.getHeaderName())) {
                        return ResponseEntity
                                        .status(HttpStatus.UNAUTHORIZED)
                                        .body(ApiResponse.error(
                                                        ErrorCode.UNAUTHENTICATED.getCode(),
                                                        "Vui lòng đăng nhập để thực hiện hành động này",
                                                        null,
                                                        request.getRequestURI()));
                }

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(
                                                HttpStatus.BAD_REQUEST.value(),
                                                "Thiếu header bắt buộc: " + ex.getHeaderName(),
                                                null,
                                                request.getRequestURI()));
        }

        /**
         * Xử lý upload file vượt quá kích thước cho phép.
         */
        @ExceptionHandler(MaxUploadSizeExceededException.class)
        public ResponseEntity<ApiResponse<Object>> handleMaxUploadSize(
                        MaxUploadSizeExceededException ex, HttpServletRequest request) {

                log.warn("File too large: {}, path={}", ex.getMessage(), request.getRequestURI());

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(
                                                ErrorCode.FILE_TOO_LARGE.getCode(),
                                                ErrorCode.FILE_TOO_LARGE.getMessage(),
                                                null,
                                                request.getRequestURI()));
        }

        /**
         * Xử lý kiểu tham số không đúng (VD: UUID parse fail).
         */
        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(
                        MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

                String message = String.format("Tham số '%s' có giá trị '%s' không hợp lệ",
                                ex.getName(), ex.getValue());

                log.warn("Type mismatch: {}, path={}", message, request.getRequestURI());

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(
                                                HttpStatus.BAD_REQUEST.value(),
                                                message,
                                                null,
                                                request.getRequestURI()));
        }

        /**
         * Xử lý thiếu query parameter bắt buộc.
         */
        @ExceptionHandler(MissingServletRequestParameterException.class)
        public ResponseEntity<ApiResponse<Object>> handleMissingParam(
                        MissingServletRequestParameterException ex, HttpServletRequest request) {

                String message = String.format("Thiếu tham số bắt buộc: %s (%s)",
                                ex.getParameterName(), ex.getParameterType());

                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(
                                                HttpStatus.BAD_REQUEST.value(),
                                                message,
                                                null,
                                                request.getRequestURI()));
        }

        /**
         * Xử lý HTTP method không được hỗ trợ.
         */
        @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
        public ResponseEntity<ApiResponse<Object>> handleMethodNotSupported(
                        HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

                return ResponseEntity
                                .status(HttpStatus.METHOD_NOT_ALLOWED)
                                .body(ApiResponse.error(
                                                HttpStatus.METHOD_NOT_ALLOWED.value(),
                                                "HTTP method " + ex.getMethod() + " không được hỗ trợ cho endpoint này",
                                                null,
                                                request.getRequestURI()));
        }

        /**
         * Xử lý resource không tìm thấy (404).
         */
        @ExceptionHandler(NoResourceFoundException.class)
        public ResponseEntity<ApiResponse<Object>> handleNoResourceFound(
                        NoResourceFoundException ex, HttpServletRequest request) {

                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.error(
                                                HttpStatus.NOT_FOUND.value(),
                                                "Endpoint không tồn tại: " + request.getRequestURI(),
                                                null,
                                                request.getRequestURI()));
        }

        /**
         * Fallback: Xử lý tất cả exception chưa được handle ở trên.
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Object>> handleGenericException(
                        Exception ex, HttpServletRequest request) {

                log.error("Unhandled exception: path={}, error={}",
                                request.getRequestURI(), ex.getMessage(), ex);

                return ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ApiResponse.error(
                                                ErrorCode.UNCATEGORIZED_EXCEPTION.getCode(),
                                                ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage(),
                                                null,
                                                request.getRequestURI()));
        }
}
