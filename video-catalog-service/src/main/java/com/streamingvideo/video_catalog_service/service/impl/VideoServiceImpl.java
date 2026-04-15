package com.streamingvideo.video_catalog_service.service.impl;

import com.streamingvideo.common.dto.event.VideoUploadedEvent;
import com.streamingvideo.common.dto.response.PageResponse;
import com.streamingvideo.common.exception.AppException;
import com.streamingvideo.common.exception.ErrorCode;
import com.streamingvideo.common.exception.VideoNotFoundException;
import com.streamingvideo.video_catalog_service.dto.request.VideoUpdateRequest;
import com.streamingvideo.video_catalog_service.dto.request.VideoUploadRequest;
import com.streamingvideo.video_catalog_service.dto.response.VideoResponse;
import com.streamingvideo.video_catalog_service.entity.Category;
import com.streamingvideo.video_catalog_service.entity.Video;
import com.streamingvideo.video_catalog_service.enums.VideoStatus;
import com.streamingvideo.video_catalog_service.enums.Visibility;
import com.streamingvideo.video_catalog_service.mapper.VideoMapper;
import com.streamingvideo.video_catalog_service.repository.CategoryRepository;
import com.streamingvideo.video_catalog_service.repository.VideoRepository;
import com.streamingvideo.video_catalog_service.service.KafkaEventPublisher;
import com.streamingvideo.video_catalog_service.service.MinioStorageService;
import com.streamingvideo.video_catalog_service.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {
    private final VideoRepository videoRepository;
    private final CategoryRepository categoryRepository;
    private final MinioStorageService minioStorageService;
    private final KafkaEventPublisher kafkaEventPublisher;

    /**
     * Upload new video
     * <p>
     * Flow:
     * 1. Upload original file to MinIO bucket "videos-raw"
     * 2. Save metadat video to PostgreSQL (status = PENDING)
     * 3. Pulbish event "video-uploaded" to Kafka
     * 4. Return response (HTTP 202 Accepted)
     *
     * @param file    Video file (Multipart)
     * @param request Metadata (title, description, tags...)
     * @param userId  ID người upload (từ Gateway header)
     */
    @Transactional
    @Override
    public VideoResponse uploadVideo(MultipartFile file, VideoUploadRequest request, UUID userId) {
        log.info("Uploading video: title={}, user={}, size={}",
                request.getTitle(), userId, file.getSize());

        // ── Step 1: Upload file to MinIO ──
        String objectName = minioStorageService.uploadRawVideo(file);
        String rawFileUrl = "videos-raw/" + objectName;

        // ── Step 2: Create metadata video ──
        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId()).orElse(null);
        }
        Video video = Video.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .userId(userId)
                .category(category)
                .tags(request.getTags())
                .originalFileName(file.getOriginalFilename())
                .originalFileUrl(rawFileUrl)
                .fileSize(file.getSize())
                .status(VideoStatus.PENDING)
                .visibility(request.getVisibility() != null
                        ? Visibility.valueOf(request.getVisibility())
                        : Visibility.PUBLIC)
                .build();
        video = videoRepository.save(video);
        log.info("Video metadata saved: videoId={}", video.getId());

        // ── Step 3: Publish Kafka event ──
        VideoUploadedEvent event = VideoUploadedEvent.builder()
                .videoId(video.getId())
                .userId(userId)
                .originalFileUrl(rawFileUrl)
                .originalFileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .timestamp(Instant.now().toString())
                .build();
        kafkaEventPublisher.publishVideoUploadedEvent(event);
        return VideoMapper.toVideoResponse(video);
    }

    @Override
    public VideoResponse getVideoById(UUID videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new VideoNotFoundException(videoId));
        return VideoMapper.toVideoResponse(video);
    }

    @Override
    public PageResponse<VideoResponse> getPublicVideos(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Video> videos = videoRepository.findByVisibilityAndStatusOrderByCreatedAtDesc(
                Visibility.PUBLIC, VideoStatus.READY, pageable);
        return PageResponse.of(videos, VideoMapper::toVideoResponse);
    }

    @Override
    public PageResponse<VideoResponse> getTrendingVideos(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Video> videos = videoRepository.findTrending(pageable);
        return PageResponse.of(videos, VideoMapper::toVideoResponse);
    }

    @Override
    public PageResponse<VideoResponse> searchVideos(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Video> videos = videoRepository.searchByTitle(keyword, pageable);
        return PageResponse.of(videos, VideoMapper::toVideoResponse);
    }

    @Override
    public PageResponse<VideoResponse> getUserVideos(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Video> videos = videoRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.of(videos, VideoMapper::toVideoResponse);
    }

    /**
     * Update metadata video
     * Only the new owner has been updated.
     */
    @Transactional
    @Override
    public VideoResponse updateVideo(UUID videoId,
                                     VideoUpdateRequest request,
                                     UUID userId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new VideoNotFoundException(videoId));

        if (!video.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.VIDEO_OWNERSHIP_REQUIRED);
        }
        VideoMapper.updateVideo(video, request);
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElse(null);
            video.setCategory(category);
        }
        video = videoRepository.save(video);
        return VideoMapper.toVideoResponse(video);
    }

    /**
     * Delete Video
     * Delete file in MinIO and metadata in DB
     */
    @Transactional
    @Override
    public void deleteVideo(UUID videoId, UUID userId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new VideoNotFoundException(videoId));
        if (!video.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.VIDEO_OWNERSHIP_REQUIRED);
        }

        if (video.getOriginalFileUrl() != null) {
            minioStorageService.deleteObject("videos-raw",
                    video.getOriginalFileUrl().replace("videos-raw/", ""));
        }
        videoRepository.delete(video);
        log.info("Deleted video: videoId={}", videoId);
    }

    /**
     * Update status after transcoding
     * Call from TranscodeEventConsumer
     */
    @Transactional
    @Override
    public void updateTranscodeResult(UUID videoId, String hlsUrl, String thumbnailUrl,
                                      Integer duration, String resolution,
                                      boolean success, String errorMessage) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new VideoNotFoundException(videoId));
        if (success) {
            video.setHlsUrl(hlsUrl);
            video.setThumbnailUrl(thumbnailUrl);
            video.setDuration(duration);
            video.setResolution(resolution);
            video.setStatus(VideoStatus.READY);
        } else {
            video.setStatus(VideoStatus.FAILED);
            log.error("Video transcoding failed: videoId={}, error={}", videoId, errorMessage);
        }
        videoRepository.save(video);
    }
}
