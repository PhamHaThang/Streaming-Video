package com.streamingvideo.common.exception;

import java.util.UUID;

/**
 * Exception khi không tìm thấy video.
 * Có thể sử dụng trực tiếp hoặc dùng AppException(ErrorCode.VIDEO_NOT_FOUND).
 */
public class VideoNotFoundException extends RuntimeException {

    private final ErrorCode errorCode;

    public VideoNotFoundException(String message) {
        super(message);
        this.errorCode = ErrorCode.VIDEO_NOT_FOUND;
    }

    public VideoNotFoundException(UUID videoId) {
        super("Không tìm thấy video: " + videoId);
        this.errorCode = ErrorCode.VIDEO_NOT_FOUND;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
