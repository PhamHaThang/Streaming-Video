package com.streamingvideo.common.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Event DTO dùng chung: Video đã được upload.
 *
 * Producer: Video Catalog Service
 * Consumer: Transcoding Service
 * Topic: "video-uploaded"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoUploadedEvent {
    private UUID videoId;
    private UUID userId;
    private String originalFileUrl;
    private String originalFileName;
    private Long fileSize;
    private String timestamp;
}
