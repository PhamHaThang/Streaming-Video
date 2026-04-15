package com.streamingvideo.common.exception;

/**
 * Exception khi upload file thất bại.
 * Có thể sử dụng trực tiếp hoặc dùng
 * AppException(ErrorCode.FILE_UPLOAD_FAILED).
 */
public class FileUploadException extends RuntimeException {

    private final ErrorCode errorCode;

    public FileUploadException(String message) {
        super(message);
        this.errorCode = ErrorCode.FILE_UPLOAD_FAILED;
    }

    public FileUploadException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCode.FILE_UPLOAD_FAILED;
    }

    public FileUploadException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
