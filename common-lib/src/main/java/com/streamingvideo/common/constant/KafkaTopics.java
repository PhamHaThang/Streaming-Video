package com.streamingvideo.common.constant;

/**
 * Hằng số Kafka Topics dùng chung.
 * Tất cả services import từ đây để tránh hardcode string.
 */
public final class KafkaTopics {

    private KafkaTopics() {
        // Prevent instantiation
    }

    /** Video Catalog → Transcoding: Video mới cần transcode */
    public static final String VIDEO_UPLOADED = "video-uploaded";

    /** Transcoding → Video Catalog: Kết quả transcode */
    public static final String TRANSCODE_COMPLETED = "transcode-completed";

    /** Interaction → Recommendation: Tương tác người dùng */
    public static final String USER_INTERACTIONS = "user-interactions";
}
