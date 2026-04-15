package com.streamingvideo.video_catalog_service.controller;

import com.streamingvideo.common.constant.AppHeaders;
import com.streamingvideo.common.dto.response.ApiResponse;
import com.streamingvideo.common.dto.response.PageResponse;
import com.streamingvideo.video_catalog_service.dto.request.VideoUpdateRequest;
import com.streamingvideo.video_catalog_service.dto.request.VideoUploadRequest;
import com.streamingvideo.video_catalog_service.dto.response.VideoResponse;
import com.streamingvideo.video_catalog_service.service.VideoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {
    private final VideoService videoService;

    /**
     * [POST] /api/videos/upload
     * Upload new video (multipart/form-data).
     * Return 202 Accepted (transcoding handles asynchronous)
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VideoResponse>> uploadVideo(
            @RequestPart("file") MultipartFile file,
            @RequestPart("metadata") @Valid VideoUploadRequest metadata,
            @RequestHeader(AppHeaders.X_USER_ID) String userId
    ) {
        VideoResponse response = videoService.uploadVideo(
                file, metadata, UUID.fromString(userId)
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Video đang được xử lý", response));
    }

    /**
     * [GET] /api/videos/{id}
     * Get info video by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VideoResponse>> getVideoById(@PathVariable UUID id) {
        VideoResponse response = videoService.getVideoById(id);
        return ResponseEntity.ok(
                ApiResponse.success("Lấy thông tin video thành công", response));
    }

    /**
     * [GET] /api/videos/public
     * Get list public videos (pagination)
     */
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<PageResponse<VideoResponse>>> getPublicVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        PageResponse<VideoResponse> response = videoService.getPublicVideos(page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Lấy danh sách public video thành công", response));
    }

    /**
     * [GET] /api/videos/trending
     * Get list trending videos (pagination)
     */
    @GetMapping("/trending")
    public ResponseEntity<ApiResponse<PageResponse<VideoResponse>>> getTrendingVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        PageResponse<VideoResponse> response = videoService.getTrendingVideos(page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Lấy danh sách trending video thành công", response));
    }

    /**
     * [GET] /api/videos/search?keyword=...
     * Search video by keyword
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<VideoResponse>>> searchVideos(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        PageResponse<VideoResponse> response = videoService.searchVideos(keyword, page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Tìm kiếm video thành công", response));
    }

    /**
     * [GET] /api/videos/my-videos
     * Get videos of current user
     */
    @GetMapping("/my-videos")
    public ResponseEntity<ApiResponse<PageResponse<VideoResponse>>> getMyVideos(
            @RequestHeader(AppHeaders.X_USER_ID) String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        PageResponse<VideoResponse> response = videoService.getUserVideos(
                UUID.fromString(userId), page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Lấy danh sách video của người dùng thành công", response));
    }

    /**
     * [PUT] /api/videos/{id}
     * Update metadata video
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VideoResponse>> updateVideo(
            @PathVariable UUID id,
            @Valid @RequestBody VideoUpdateRequest request,
            @RequestHeader(AppHeaders.X_USER_ID) String userId) {
        VideoResponse response = videoService.updateVideo(
                id, request, UUID.fromString(userId));
        return ResponseEntity.ok(
                ApiResponse.success("Cập nhật video thành công", response));
    }

    /**
     * [DELETE] /api/videos/{id}
     * Delete video
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<VideoResponse>> updateVideo(
            @PathVariable UUID id,
            @RequestHeader(AppHeaders.X_USER_ID) String userId) {
        videoService.deleteVideo(id, UUID.fromString(userId));
        return ResponseEntity.ok(
                ApiResponse.success("Xóa video với id: " + id + " thành công", null));
    }

}
