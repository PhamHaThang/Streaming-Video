package com.streamingvideo.video_catalog_service.service;

import com.streamingvideo.common.dto.response.PageResponse;
import com.streamingvideo.video_catalog_service.dto.request.VideoUpdateRequest;
import com.streamingvideo.video_catalog_service.dto.request.VideoUploadRequest;
import com.streamingvideo.video_catalog_service.dto.response.VideoResponse;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface VideoService {
    public VideoResponse uploadVideo(MultipartFile file,
                                     VideoUploadRequest request,
                                     UUID userId);

    public VideoResponse getVideoById(UUID videoId);

    public PageResponse<VideoResponse> getPublicVideos(int page, int size);

    public PageResponse<VideoResponse> getTrendingVideos(int page, int size);

    public PageResponse<VideoResponse> searchVideos(String keyword, int page, int size);

    public PageResponse<VideoResponse> getUserVideos(UUID userId, int page, int size);

    public VideoResponse updateVideo(UUID videoId, VideoUpdateRequest request, UUID userId);

    public void deleteVideo(UUID videoId, UUID userId);

    public void updateTranscodeResult(
            UUID videoId, String hlsUrl,
            String thumbnailUrl, Integer duration,
            String resolution, boolean success,
            String errorMessage
    );


}
