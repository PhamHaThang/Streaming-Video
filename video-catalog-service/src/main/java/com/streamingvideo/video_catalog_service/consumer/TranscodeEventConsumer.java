package com.streamingvideo.video_catalog_service.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamingvideo.common.constant.KafkaTopics;
import com.streamingvideo.common.dto.event.TranscodeCompletedEvent;
import com.streamingvideo.video_catalog_service.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka Consumer: Listen transcoding result
 * <p>
 * When Transcoding Service process video completely
 * -> publish event to topic "transcode-completed"
 * <p>
 * TranscodeEventConsumer receive event and update metadata video (HLS URL, duration, status...).
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class TranscodeEventConsumer {
    private final VideoService videoService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = KafkaTopics.TRANSCODE_COMPLETED,
            groupId = "video-catalog-group"
    )
    public void handleTranscodeCompleted(String message) {
        try {
            TranscodeCompletedEvent event = objectMapper.readValue(
                    message, TranscodeCompletedEvent.class
            );
            log.info("Received transcode-completed event: videoId={}, success={}",
                    event.getVideoId(), event.isSuccess());
            videoService.updateTranscodeResult(
                    event.getVideoId(),
                    event.getHlsUrl(),
                    event.getThumbnailUrl(),
                    event.getDuration(),
                    event.getResolution(),
                    event.isSuccess(),
                    event.getErrorMessage()
            );
        } catch (Exception e) {
            log.error("Failed to process transcode-completed event", e);
        }
    }
}
