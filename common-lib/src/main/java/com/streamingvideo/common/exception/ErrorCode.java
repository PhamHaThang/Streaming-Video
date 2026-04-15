package com.streamingvideo.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Enum - All error code in system.
 *
 * Convention error code:
 * - 4xxx: Client errors
 * - 40xx: Validation / Bad Request
 * - 401x: Authentication
 * - 403x: Authorization / Forbidden
 * - 404x: Not Found
 * - 409x: Conflict (duplicate)
 * - 5xxx: Server errors
 * - 50xx: Internal / Infrastructure
 */
@Getter
public enum ErrorCode {

    // ═══════════════════════════════════════
    // General
    // ═══════════════════════════════════════
    UNCATEGORIZED_EXCEPTION(5000, "Lỗi không xác định", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_ERROR(4000, "Xác thực dữ liệu thất bại", HttpStatus.BAD_REQUEST),
    INVALID_KEY(4001, "Khóa tin nhắn không hợp lệ", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST(4002, "Yêu cầu không hợp lệ", HttpStatus.BAD_REQUEST),

    // ═══════════════════════════════════════
    // AUTHENTICATION (401x)
    // ═══════════════════════════════════════
    UNAUTHENTICATED(4010, "Chưa xác thực", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN(4011, "Token không hợp lệ", HttpStatus.UNAUTHORIZED),
    EXPIRED_TOKEN(4012, "Token đã hết hạn", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN(4013, "Refresh token không hợp lệ", HttpStatus.UNAUTHORIZED),
    EXPIRED_REFRESH_TOKEN(4014, "Refresh token đã hết hạn. Vui lòng đăng nhập lại", HttpStatus.UNAUTHORIZED),

    // ═══════════════════════════════════════
    // AUTHORIZATION (403x)
    // ═══════════════════════════════════════
    UNAUTHORIZED(4030, "Không có quyền truy cập", HttpStatus.FORBIDDEN),
    ACCOUNT_DISABLED(4031, "Tài khoản đã bị vô hiệu hóa", HttpStatus.FORBIDDEN),
    ACCESS_DENIED(4032, "Bạn không có quyền thực hiện hành động này", HttpStatus.FORBIDDEN),

    // ═══════════════════════════════════════
    // USER SERVICE (404x, 409x)
    // ═══════════════════════════════════════
    USER_NOT_FOUND(4040, "Người dùng không tồn tại", HttpStatus.NOT_FOUND),
    USER_EXISTED(4090, "Tên người dùng đã tồn tại", HttpStatus.CONFLICT),
    EMAIL_EXISTED(4091, "Email đã được sử dụng", HttpStatus.CONFLICT),
    INVALID_PASSWORD(4003, "Mật khẩu hiện tại không đúng", HttpStatus.BAD_REQUEST),
    INVALID_CREDENTIALS(4004, "Email/Username hoặc mật khẩu không đúng", HttpStatus.BAD_REQUEST),

    // ═══════════════════════════════════════
    // VIDEO CATALOG SERVICE
    // ═══════════════════════════════════════
    VIDEO_NOT_FOUND(4041, "Không tìm thấy video", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND(4042, "Không tìm thấy danh mục", HttpStatus.NOT_FOUND),
    VIDEO_NOT_READY(4005, "Video chưa sẵn sàng để phát", HttpStatus.BAD_REQUEST),
    VIDEO_OWNERSHIP_REQUIRED(4033, "Bạn không phải chủ sở hữu video này", HttpStatus.FORBIDDEN),

    // ═══════════════════════════════════════
    // INTERACTION SERVICE
    // ═══════════════════════════════════════
    INTERACTION_NOT_FOUND(4043, "Không tìm thấy tương tác", HttpStatus.NOT_FOUND),
    INVALID_INTERACTION_TYPE(4006, "Loại tương tác không hợp lệ", HttpStatus.BAD_REQUEST),
    ALREADY_LIKED(4092, "Bạn đã thích video này rồi", HttpStatus.CONFLICT),

    // ═══════════════════════════════════════
    // FILE / MINIO / UPLOAD
    // ═══════════════════════════════════════
    FILE_UPLOAD_FAILED(5001, "Tải file lên thất bại", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_TOO_LARGE(4007, "File vượt quá kích thước cho phép", HttpStatus.BAD_REQUEST),
    INVALID_FILE_TYPE(4008, "Định dạng file không được hỗ trợ", HttpStatus.BAD_REQUEST),
    MINIO_INIT_ERROR(5002, "MinIO khởi tạo lỗi", HttpStatus.INTERNAL_SERVER_ERROR),
    MINIO_OPERATION_ERROR(5003, "Lỗi thao tác MinIO", HttpStatus.INTERNAL_SERVER_ERROR),

    // ═══════════════════════════════════════
    // TRANSCODING SERVICE
    // ═══════════════════════════════════════
    TRANSCODE_FAILED(5004, "Chuyển đổi video thất bại", HttpStatus.INTERNAL_SERVER_ERROR),
    FFMPEG_ERROR(5005, "Lỗi FFmpeg khi xử lý video", HttpStatus.INTERNAL_SERVER_ERROR),
    TRANSCODE_TIMEOUT(5006, "Chuyển đổi video vượt quá thời gian cho phép", HttpStatus.INTERNAL_SERVER_ERROR),

    // ═══════════════════════════════════════
    // KAFKA / MESSAGING
    // ═══════════════════════════════════════
    KAFKA_PUBLISH_ERROR(5010, "Lỗi khi gửi message tới Kafka", HttpStatus.INTERNAL_SERVER_ERROR),
    KAFKA_CONSUME_ERROR(5011, "Lỗi khi đọc message từ Kafka", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus statusCode;

    ErrorCode(int code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}