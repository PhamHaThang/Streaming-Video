package com.streamingvideo.transcoding_service.service;

import com.streamingvideo.common.dto.event.TranscodeCompletedEvent;
import com.streamingvideo.common.dto.event.VideoUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranscodingService {
    private final FFmpegService ffmpegService;
    private final MinioStorageService minioStorageService;
    private final KafkaEventPublisher kafkaEventPublisher;

    @Value("${transcoding.temp-dir:./temp/transcode}")
    private String tempDir;

    /**
     * Process all transcoding progress for 1 video
     * Flow:
     * 1. Download original video form MinIO
     * 2. Create Thumbnail
     * 3. Transcode to HLS (360p,720p,1080p)
     * 4. Create master playlist
     * 5. Upload all output to MinIO
     * 6. Clean temp files
     * 7. Publish result event
     */
    public void processVideo(VideoUploadedEvent event) {
        String videoId = event.getVideoId().toString();
        Path workDir = Paths.get(tempDir, videoId);

        log.info("══════════════════════════════════════════");
        log.info("Starting transcoding: videoId={}", videoId);
        log.info("══════════════════════════════════════════");
        try {
            // ── Step 1: Create temp dir ──
            Files.createDirectories(workDir);
            Path inputFile = workDir.resolve("input_video");

            // ── Step 2: Download original video form MinIO ──
            log.info("[1/7] Downloading raw video from MinIO...");
            minioStorageService.downloadFile("videos-raw",
                    event.getOriginalFileUrl().replace("videos-raw/", ""),
                    inputFile);

            // ── Step 3: Get video info ──
            log.info("[2/7] Getting video info...");
            int duration = ffmpegService.getVideoDuration(inputFile);
            String resolution = ffmpegService.getVideoResolution(inputFile);
            log.info("  Duration: {}s | Resolution: {}", duration, resolution);

            // ── Step 4: Create Thumbnail ──
            log.info("[3/7] Extracting thumbnail...");
            Path thumbnailFile = workDir.resolve("thumbnail.jpg");
            ffmpegService.extractThumbnail(inputFile, thumbnailFile);

            // ── Step 5: Transcode HLS - 360p ──
            log.info("[4/7] Transcoding to 360p...");
            Path dir360p = workDir.resolve("hls/360p");
            ffmpegService.transcodeToHls(inputFile, dir360p, 360, "800k", "96k");

            // ── Step 6: Transcode HLS - 720p ──
            log.info("[5/7] Transcoding to 720p...");
            Path dir720p = workDir.resolve("hls/720p");
            ffmpegService.transcodeToHls(inputFile, dir720p, 720, "2500k", "128k");

            // ── Step 7: Transcode HLS - 1080p ──
            log.info("[6/7] Transcoding to 1080p...");
            Path dir1080p = workDir.resolve("hls/1080p");
            ffmpegService.transcodeToHls(inputFile, dir1080p, 1080, "5000k", "192k");

            // ── Step 8: Create master playlist ──
            log.info("[7/7] Creating master playlist...");
            Path hlsDir = workDir.resolve("hls");
            ffmpegService.createMasterPlaylist(hlsDir, videoId);

            // ── Step 9: Upload all output to MinIO ──
            log.info("Uploading HLS files to MinIO...");
            String hlsPrefix = videoId + "/";
            minioStorageService.uploadDirectory("videos-hls", hlsDir, hlsPrefix);

            log.info("Uploading thumbnail to MinIO...");
            String thumbnailObjectName = videoId + "/thumbnail.jpg";
            minioStorageService.uploadFile("thumbnails", thumbnailFile, thumbnailObjectName);

            // ── Step 10: Publish event ──
            String hlsUrl = hlsPrefix + "master.m3u8";

            TranscodeCompletedEvent completedEvent = TranscodeCompletedEvent.builder()
                    .videoId(event.getVideoId())
                    .hlsUrl(hlsUrl)
                    .thumbnailUrl(thumbnailObjectName)
                    .duration(duration)
                    .resolution(resolution)
                    .success(true)
                    .timestamp(Instant.now().toString())
                    .build();
            kafkaEventPublisher.publishTranscodeCompleted(completedEvent);

            log.info("══════════════════════════════════════════");
            log.info("Transcoding COMPLETED: videoId={}", videoId);
            log.info("  HLS URL: {}", hlsUrl);
            log.info("  Duration: {}s", duration);
            log.info("══════════════════════════════════════════");
        } catch (Exception e) {
            log.error("Transcoding FAILED: videoId={}", videoId, e);
            // Publish error event
            TranscodeCompletedEvent failedEvent = TranscodeCompletedEvent.builder()
                    .videoId(event.getVideoId())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .timestamp(Instant.now().toString())
                    .build();
            kafkaEventPublisher.publishTranscodeCompleted(failedEvent);
        } finally {
            // Clean temp dir
            cleanupTempDir(workDir);
        }
    }

    private void cleanupTempDir(Path dir) {
        try {
            if (Files.exists(dir)) {
                Files.walk(dir)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (Exception ignored) {
                            }
                        });
                log.info("Cleaned up temp directory: {}", dir);
            }
        } catch (Exception e) {
            log.warn("Failed to cleanup temp directory: {}", dir, e);
        }
    }
}
