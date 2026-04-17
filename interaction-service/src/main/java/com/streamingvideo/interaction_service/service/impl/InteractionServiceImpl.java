package com.streamingvideo.interaction_service.service.impl;

import com.streamingvideo.common.dto.event.UserInteractionEvent;
import com.streamingvideo.common.dto.response.PageResponse;
import com.streamingvideo.common.exception.AppException;
import com.streamingvideo.common.exception.ErrorCode;
import com.streamingvideo.interaction_service.dto.request.InteractionRequest;
import com.streamingvideo.interaction_service.dto.request.WatchTimeRequest;
import com.streamingvideo.interaction_service.dto.response.InteractionResponse;
import com.streamingvideo.interaction_service.dto.response.UserStatsResponse;
import com.streamingvideo.interaction_service.dto.response.WatchHistoryResponse;
import com.streamingvideo.interaction_service.entity.Interaction;
import com.streamingvideo.interaction_service.entity.Like;
import com.streamingvideo.interaction_service.entity.WatchHistory;
import com.streamingvideo.interaction_service.enums.InteractionType;
import com.streamingvideo.interaction_service.mapper.InteractionMapper;
import com.streamingvideo.interaction_service.mapper.WatchHistoryMapper;
import com.streamingvideo.interaction_service.repository.InteractionRepository;
import com.streamingvideo.interaction_service.repository.LikeRepository;
import com.streamingvideo.interaction_service.repository.WatchHistoryRepository;
import com.streamingvideo.interaction_service.service.InteractionService;
import com.streamingvideo.interaction_service.service.KafkaEventPublisher;
import com.streamingvideo.interaction_service.service.RedisCounterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class InteractionServiceImpl implements InteractionService {
    private final InteractionRepository interactionRepository;
    private final WatchHistoryRepository watchHistoryRepository;
    private final LikeRepository likeRepository;
    private final RedisCounterService redisCounterService;
    private final KafkaEventPublisher kafkaEventPublisher;

    /**
     * Ghi nhận tương tác chung (VIEW, SHARE, COMMENT...).
     */
    @Transactional
    @Override
    public InteractionResponse recordInteraction(InteractionRequest interactionRequest, UUID userId) {
        log.info("Recording interaction: userId={}, videoId={}, type={}",
                userId, interactionRequest.getVideoId(), interactionRequest.getInteractionType());

        // ── Step 1: Save to DB ──
        Interaction interaction = InteractionMapper.toInteraction(interactionRequest);

        // LIKE/UNLIKE must go through toggle endpoint to keep likes table and counters consistent.
        if (interaction.getInteractionType() == InteractionType.LIKE
                || interaction.getInteractionType() == InteractionType.UNLIKE) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Use /api/interactions/like/{videoId} for LIKE/UNLIKE actions");
        }

        interaction.setUserId(userId);
        interaction = interactionRepository.save(interaction);

        // ── Step 2: Update to Redis counters ──
        redisCounterService.incrementCounter(interactionRequest.getVideoId(), interaction.getInteractionType());
        if (interaction.getInteractionType() == InteractionType.VIEW) {
            WatchHistory history = watchHistoryRepository
                    .findByUserIdAndVideoId(userId, interactionRequest.getVideoId())
                    .orElse(WatchHistory.builder()
                            .userId(userId)
                            .videoId(interactionRequest.getVideoId())
                            .watchCount(0) 
                            .build());

            // Mỗi lần gửi sự kiện VIEW, ta cộng thêm 1 lần xem
            history.setWatchCount(history.getWatchCount() + 1);
            watchHistoryRepository.save(history);
        }
        // ── Step 3: Publish Kafka event ──
        UserInteractionEvent event = UserInteractionEvent.builder()
                .userId(userId)
                .videoId(interactionRequest.getVideoId())
                .interactionType(interaction.getInteractionType().name())
                .weight(interaction.getInteractionType().getWeight())
                .metadata(interactionRequest.getMetadata())
                .timestamp(Instant.now().toString())
                .build();

        kafkaEventPublisher.publishInteractionEvent(event);

        return InteractionMapper.toInteractionResponse(interaction);
    }

    /**
     * Toggle like/unlike
     */
    @Transactional
    @Override
    public boolean toggleLike(UUID videoId, UUID userId) {
        boolean exists = likeRepository.existsByUserIdAndVideoId(userId, videoId);
        if (exists) {
            // ── Unlike ──
            likeRepository.deleteByUserIdAndVideoId(userId, videoId);
            recordInteractionInternal(userId, videoId, InteractionType.UNLIKE);
            redisCounterService.decrementCounter(videoId, InteractionType.LIKE);
            log.info("User {} unliked video {}", userId, videoId);
            return false; // Unlike
        } else {
            // ── Like ──
            Like like = Like.builder()
                    .videoId(videoId)
                    .userId(userId)
                    .build();
            likeRepository.save(like);
            recordInteractionInternal(userId, videoId, InteractionType.LIKE);
            redisCounterService.incrementCounter(videoId, InteractionType.LIKE);
            log.info("User {} liked video {}", userId, videoId);
            return true; // Like
        }
    }

    /**
     * Update watch time
     * Su dung UPSERT: Tao moi or update neu da ton tai
     */
    @Transactional
    @Override
    public void updateWatchTime(WatchTimeRequest request, UUID userId) {
        WatchHistory history = watchHistoryRepository
                .findByUserIdAndVideoId(userId, request.getVideoId())
                .orElse(WatchHistory.builder()
                        .videoId(request.getVideoId())
                        .userId(userId)
                        .watchCount(0)
                        .build()
                );

        int requestWatchDuration = request.getWatchDuration() != null ? request.getWatchDuration() : 0;
        int requestVideoDuration = request.getVideoDuration() != null ? request.getVideoDuration() : 0;
        int requestLastPosition = request.getLastPosition() != null ? request.getLastPosition() : 0;

        BigDecimal oldPercentage = history.getWatchPercentage();
        // Cap nhat thong tin xem
        int maxDuration = Math.max(history.getWatchDuration() != null ? history.getWatchDuration() : 0,
                requestWatchDuration);
        history.setWatchDuration(maxDuration);
        history.setVideoDuration(requestVideoDuration);
        history.setLastPosition(requestLastPosition);

        // Tinh % da xem
        if (requestVideoDuration > 0) {
            BigDecimal currentPercentage = BigDecimal.valueOf(maxDuration)
                    .divide(BigDecimal.valueOf(requestVideoDuration), 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            history.setWatchPercentage(currentPercentage);

            // Chi ghi nhan COMPLETE neu % hien tai >= 90 % va truoc do chua dat 90 %
            boolean isJustCompleted = currentPercentage.compareTo(BigDecimal.valueOf(90)) >= 0
                    && (oldPercentage == null || oldPercentage.compareTo(BigDecimal.valueOf(90)) < 0);
            if (isJustCompleted) {
                recordInteractionInternal(userId, request.getVideoId(), InteractionType.COMPLETE);
            }
        }

        watchHistoryRepository.save(history);

        // Publish watch time event cho recommendation
        UserInteractionEvent event = UserInteractionEvent.builder()
                .userId(userId)
                .videoId(request.getVideoId())
                .interactionType(InteractionType.WATCH_TIME.name())
                .weight(calculateWatchTimeWeight(history.getWatchPercentage()))
                .timestamp(Instant.now().toString())
                .build();
        kafkaEventPublisher.publishInteractionEvent(event);
    }

    /**
     * Lấy thống kê tuơng tác của user
     */
    @Override
    public UserStatsResponse getUserStats(UUID userId) {
        long totalViews = interactionRepository.countByUserIdAndInteractionType(userId, InteractionType.VIEW);
        long totalLikes = likeRepository.countByUserId(userId);
        long totalWatched = watchHistoryRepository.countByUserId(userId);

        Long totalWatchTime = watchHistoryRepository.sumWatchTimeByUserId(userId);
        long watchTimeSeconds = (totalWatchTime != null) ? totalWatchTime : 0L;
        return UserStatsResponse.builder()
                .totalViews(totalViews)
                .totalLikes(totalLikes)
                .videosWatched(totalWatched)
                .totalWatchTime(watchTimeSeconds)
                .build();
    }

    /**
     * Kiểm tra user đã like video chưa
     */
    @Override
    public boolean hasUserLiked(UUID videoId, UUID userId) {
        return likeRepository.existsByUserIdAndVideoId(userId, videoId);
    }

    @Override
    public PageResponse<WatchHistoryResponse> getUserWatchHistory(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(
                page, size, Sort.by("updatedAt").descending());
        Page<WatchHistory> watchHistoryPage = watchHistoryRepository
                .findByUserIdOrderByUpdatedAtDesc(userId, pageable);
        return PageResponse.of(watchHistoryPage, WatchHistoryMapper::toWatchHistoryResponse);
    }

    // =============== Helper ====================
    private void recordInteractionInternal(UUID userId, UUID videoId, InteractionType interactionType) {
        Interaction interaction = Interaction.builder()
                .videoId(videoId)
                .interactionType(interactionType)
                .userId(userId)
                .build();
        interaction = interactionRepository.save(interaction);

        UserInteractionEvent event = UserInteractionEvent.builder()
                .userId(interaction.getUserId())
                .videoId(interaction.getVideoId())
                .interactionType(interaction.getInteractionType().name())
                .weight(interaction.getInteractionType().getWeight())
                .timestamp(Instant.now().toString())
                .build();
        kafkaEventPublisher.publishInteractionEvent(event);
    }

    /**
     * Tinh weight dua tren % xem
     * Xem nhieu -> trong so cao hon
     */
    private double calculateWatchTimeWeight(BigDecimal watchPercentage) {
        if (watchPercentage == null) return 0.5;
        double pct = watchPercentage.doubleValue();
        if (pct >= 90) return 5.0;   // Xem gần hết
        if (pct >= 70) return 3.5;   // Xem phần lớn
        if (pct >= 50) return 2.5;   // Xem nửa
        if (pct >= 25) return 1.5;   // Xem 1/4
        return 0.5;                  // Xem ít
    }


}
