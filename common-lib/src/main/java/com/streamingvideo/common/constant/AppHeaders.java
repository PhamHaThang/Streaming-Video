package com.streamingvideo.common.constant;

/**
 * Hằng số HTTP Headers dùng chung.
 * API Gateway inject headers này sau khi xác thực JWT.
 */
public final class AppHeaders {

    private AppHeaders() {
        // Prevent instantiation
    }

    /** Header chứa User ID, được Gateway inject sau JWT validation */
    public static final String X_USER_ID = "X-User-Id";

    /** Header chứa User Role */
    public static final String X_USER_ROLE = "X-User-Role";

    /** Header chứa Username */
    public static final String X_USERNAME = "X-Username";
}
