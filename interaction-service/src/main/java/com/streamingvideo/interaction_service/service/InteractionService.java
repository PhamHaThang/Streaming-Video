package com.streamingvideo.interaction_service.service;

import com.streamingvideo.common.dto.response.PageResponse;
import com.streamingvideo.interaction_service.dto.request.InteractionRequest;
import com.streamingvideo.interaction_service.dto.request.WatchTimeRequest;
import com.streamingvideo.interaction_service.dto.response.InteractionResponse;
import com.streamingvideo.interaction_service.dto.response.UserStatsResponse;
import com.streamingvideo.interaction_service.dto.response.WatchHistoryResponse;

import java.util.UUID;

public interface InteractionService {
    /**
     * Ghi nhận tương tác chung
     */
    public InteractionResponse recordInteraction(InteractionRequest interactionRequest, UUID userId);

    /**
     * Toggle like/unlike
     */
    public boolean toggleLike(UUID videoId, UUID userId);

    /**
     * Update watch time
     */
    public void updateWatchTime(WatchTimeRequest request, UUID userId);

    /**
     * Lấy thống kê tuơng tác của user
     */
    public UserStatsResponse getUserStats(UUID userId);

    /**
     * Kiểm tra user đã like video chưa
     */
    public boolean hasUserLiked(UUID videoId, UUID userId);
    /**
     * Lấy lịch sử xem cua người dùng hiện tại
     */
    public PageResponse<WatchHistoryResponse> getUserWatchHistory(UUID userId, int page, int size);
}
