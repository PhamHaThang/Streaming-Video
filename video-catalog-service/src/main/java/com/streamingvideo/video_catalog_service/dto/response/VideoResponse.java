package com.streamingvideo.video_catalog_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoResponse {
    private UUID id;
    private String title;
    private String description;
    private UUID userId;
    private String categoryName;
    private List<String> tags;
    private String hlsUrl;
    private String thumbnailUrl;
    private Integer duration;
    private String resolution;
    private Long viewCount;
    private Long likeCount;
    private String status;
    private String visibility;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
