package com.streamingvideo.common.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Event DTO dùng chung: Transcoding hoàn tất.
 *
 * Producer: Transcoding Service
 * Consumer: Video Catalog Service
 * Topic: "transcode-completed"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscodeCompletedEvent {
    private UUID videoId;
    private String hlsUrl;
    private String thumbnailUrl;
    private Integer duration;
    private String resolution;
    private boolean success;
    private String errorMessage;
    private String timestamp;
}
