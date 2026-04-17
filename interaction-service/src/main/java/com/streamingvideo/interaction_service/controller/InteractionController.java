package com.streamingvideo.interaction_service.controller;

import com.streamingvideo.common.constant.AppHeaders;
import com.streamingvideo.common.dto.response.ApiResponse;
import com.streamingvideo.common.dto.response.PageResponse;
import com.streamingvideo.interaction_service.dto.request.InteractionRequest;
import com.streamingvideo.interaction_service.dto.request.WatchTimeRequest;
import com.streamingvideo.interaction_service.dto.response.InteractionResponse;
import com.streamingvideo.interaction_service.dto.response.UserStatsResponse;
import com.streamingvideo.interaction_service.dto.response.WatchHistoryResponse;
import com.streamingvideo.interaction_service.service.InteractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/interactions")
@RequiredArgsConstructor
public class InteractionController {
    private final InteractionService interactionService;

    /**
     * [POST] /api/interactions
     * Ghi nhan tuong tac (VIEW, SHARE, COMMENT,...)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<InteractionResponse>> createInteraction(
            @Valid @RequestBody InteractionRequest interactionRequest,
            @RequestHeader(AppHeaders.X_USER_ID) String userId) {
        InteractionResponse response = interactionService
                .recordInteraction(interactionRequest, UUID.fromString(userId));
        return ResponseEntity.ok(
                ApiResponse.success("Ghi nhâ tương tác thành công", response));
    }

    /**
     * [POST] /api/interactions/like/{videoId}
     * Toggle like/unlike video
     */
    @PostMapping("/like/{videoId}")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> toggleLike(
            @PathVariable("videoId") UUID videoId,
            @RequestHeader(AppHeaders.X_USER_ID) String userId
    ) {
        boolean liked = interactionService.toggleLike(videoId, UUID.fromString(userId));
        return ResponseEntity.ok(
                ApiResponse.success(liked ? "Like thành công" : " Unlike thành công",
                        Map.of("liked", liked)
                )
        );
    }

    /**
     * [GET] /api/interactions/like/{videoId}/status
     * Kiem tra user da like video chua
     */
    @GetMapping("/like/{videoId}/status")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getLikeStatus(
            @PathVariable("videoId") UUID videoId,
            @RequestHeader(AppHeaders.X_USER_ID) String userId
    ) {
        boolean liked = interactionService.hasUserLiked(videoId, UUID.fromString(userId));
        return ResponseEntity.ok(
                ApiResponse.success(liked ? "User đã like video" : " User chưa like video",
                        Map.of("liked", liked)
                )
        );
    }

    /**
     * [POST] /api/interactions/watch-time
     * Update thoi gian xem video
     * Client send request moi 10-30s
     */
    @PostMapping("/watch-time")
    public ResponseEntity<ApiResponse<Void>> updateWatchTime(
            @Valid @RequestBody WatchTimeRequest request,
            @RequestHeader(AppHeaders.X_USER_ID) String userId
    ) {
        interactionService.updateWatchTime(request, UUID.fromString(userId));
        return ResponseEntity.ok(
                ApiResponse.success("Cập nhật thời gian xem thành công", null));
    }

    /**
     * [GET] /api/interactions/stats
     * Lay thong ke tuong tac cua current user
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<UserStatsResponse>> getUserStats(
            @RequestHeader(AppHeaders.X_USER_ID) String userId
    ) {
        UserStatsResponse response = interactionService.getUserStats(UUID.fromString(userId));
        return ResponseEntity.ok(
                ApiResponse.success("Lấy thống kê tương tác của user thành công", response)
        );
    }

    /**
     * [GET] /api/interactions/history
     * Lay lich su xem cua current user
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PageResponse<WatchHistoryResponse>>> getWatchHistory(
            @RequestHeader(AppHeaders.X_USER_ID) String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<WatchHistoryResponse> response = interactionService
                .getUserWatchHistory(UUID.fromString(userId), page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Lây lịch sử xem của người dùng thành công", response));
    }

}
