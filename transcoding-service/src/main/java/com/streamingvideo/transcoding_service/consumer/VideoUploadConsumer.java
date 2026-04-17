package com.streamingvideo.transcoding_service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamingvideo.common.constant.KafkaTopics;
import com.streamingvideo.common.dto.event.VideoUploadedEvent;
import com.streamingvideo.transcoding_service.service.TranscodingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


/**
 * Kafka Consumer: Listen event video-uploaded
 *
 * When Video Catalog Service publish event after user upload video
 *
 * VideoUploadConsumer receive event and start transcoding simultaneously
 * Note:
 * - concurrency = 2: max 2 video transcoding in time
 * - If transcoding thất failed → catch exception and publish error event
 * - No infinite retry (avoid blocking Kafka consumer)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VideoUploadConsumer {
    private final TranscodingService transcodingService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = KafkaTopics.VIDEO_UPLOADED,
            groupId = "transcoding-group",
            concurrency = "2"
    )
    public void handleVideoUploaded(String message){
        try {
            VideoUploadedEvent event = objectMapper.readValue(
                    message,VideoUploadedEvent.class);
            log.info("═══════════════════════════════════════════════");
            log.info("Received video-uploaded event");
            log.info("  VideoId:  {}", event.getVideoId());
            log.info("  UserId:   {}", event.getUserId());
            log.info("  File:     {}", event.getOriginalFileName());
            log.info("  Size:     {} MB", event.getFileSize() / (1024 * 1024));
            log.info("═══════════════════════════════════════════════");

            transcodingService.processVideo(event);
        } catch (Exception e) {
            log.error("Failed to process video-uploaded event: {}", e.getMessage(), e);
        }
    }
}
