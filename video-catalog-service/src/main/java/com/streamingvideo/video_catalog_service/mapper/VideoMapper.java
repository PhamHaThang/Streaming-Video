package com.streamingvideo.video_catalog_service.mapper;

import com.streamingvideo.video_catalog_service.dto.request.VideoUpdateRequest;
import com.streamingvideo.video_catalog_service.dto.response.VideoResponse;
import com.streamingvideo.video_catalog_service.entity.Video;
import com.streamingvideo.video_catalog_service.enums.Visibility;

public class VideoMapper {
    public static VideoResponse toVideoResponse(Video video) {
        return VideoResponse.builder()
                .id(video.getId())
                .title(video.getTitle())
                .description(video.getDescription())
                .userId(video.getUserId())
                .categoryName(video.getCategory() != null ?
                        video.getCategory().getName() : null)
                .tags(video.getTags())
                .hlsUrl(video.getHlsUrl())
                .thumbnailUrl(video.getThumbnailUrl())
                .duration(video.getDuration())
                .resolution(video.getResolution())
                .viewCount(video.getViewCount())
                .likeCount(video.getLikeCount())
                .status(video.getStatus().name())
                .visibility(video.getVisibility().name())
                .createdAt(video.getCreatedAt())
                .updatedAt(video.getUpdatedAt())
                .build();
    }

    public static void updateVideo(Video video, VideoUpdateRequest request) {
        if (request == null) return;
        if (request.getTitle() != null)
            video.setTitle(request.getTitle());
        if (request.getDescription() != null)
            video.setDescription(request.getDescription());
        if (request.getTags()!=null)
            video.setTags(request.getTags());
        if (request.getVisibility() != null) {
            video.setVisibility(Visibility.valueOf(request.getVisibility()));
        }
    }
}
