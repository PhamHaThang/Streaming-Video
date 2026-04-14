package com.streamingvideo.user_service.common.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(5000, "Lỗi không xác định", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_ERROR(4000, "Xác thực thất bại", HttpStatus.BAD_REQUEST),
    INVALID_KEY(4001, "Khóa tin nhắn không hợp lệ", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(4002, "Mật khẩu hiện tại không đúng", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(4040, "Người dùng không tồn tại", HttpStatus.NOT_FOUND),
    USER_EXISTED(4090, "Người dùng đã tồn tại", HttpStatus.CONFLICT),
    EMAIL_EXISTED(4091, "Email đã tồn tại", HttpStatus.CONFLICT),
    INVALID_REFRESH_TOKEN(4003, "Refresh token không hợp lệ", HttpStatus.BAD_REQUEST),
    EXPIRED_REFRESH_TOKEN(4004, "Refresh token đã hết hạn", HttpStatus.BAD_REQUEST),
    ACCOUNT_DISABLED(4031, "Tài khoản đã bị vô hiệu hóa", HttpStatus.FORBIDDEN),
    UNAUTHENTICATED(4010, "Chưa xác thực", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(4030, "Không có quyền truy cập", HttpStatus.FORBIDDEN);

    private final int code;
    private final String message;
    private final HttpStatus statusCode;

    ErrorCode(int code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}