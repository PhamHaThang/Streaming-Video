package com.streamingvideo.interaction_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserStatsResponse {
    private long totalViews;
    private long totalLikes;
    private long totalWatchTime; // s
    private long videosWatched;
}
