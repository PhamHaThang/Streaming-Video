package com.streamingvideo.interaction_service.mapper;

import com.streamingvideo.interaction_service.dto.response.WatchHistoryResponse;
import com.streamingvideo.interaction_service.entity.WatchHistory;

public class WatchHistoryMapper {
    public static WatchHistoryResponse toWatchHistoryResponse(WatchHistory watchHistory) {
        return WatchHistoryResponse.builder()
                .id(watchHistory.getId())
                .videoId(watchHistory.getVideoId())
                .lastPosition(watchHistory.getLastPosition())
                .watchPercentage(watchHistory.getWatchPercentage() != null
                        ? watchHistory.getWatchPercentage().doubleValue() : 0.0)
                .updatedAt(watchHistory.getUpdatedAt())
                .build();
    }
}
